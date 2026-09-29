import { Router } from "express";
import { registerSchema, loginSchema, refreshSchema } from "./types.js";
import { prisma, hashPassword, verifyPassword, hashToken, ACCESS_SECRET } from "./service.js";
import jwt from "jsonwebtoken";
import crypto from "crypto";

const router = Router();

router.post("/register", async (req, res, next) => {
  try {
    const data = registerSchema.parse(req.body);
    const existing = await prisma.user.findUnique({ where: { email: data.email.toLowerCase() } });
    if (existing) {
      res.status(409).json({ error: "Email already in use" });
      return;
    }
    const pwHash = await hashPassword(data.password);
    const user = await prisma.user.create({
      data: { email: data.email.toLowerCase(), passwordHash: pwHash, name: data.name, role: data.role || "CUSTOMER", isActive: true },
      select: { id: true, email: true, name: true, role: true },
    });
    const accessToken = jwt.sign({ sub: user.id, role: user.role, email: user.email }, ACCESS_SECRET, { expiresIn: "15m" });
    const refreshRaw = crypto.randomBytes(32).toString("hex");
    const refreshHash = hashToken(refreshRaw);
    await prisma.refreshToken.create({ data: { userId: user.id, tokenHash: refreshHash, expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000) } });
    res.status(201).json({ accessToken, refreshToken: refreshRaw, user });
  } catch (e: any) {
    next(e);
  }
});

router.post("/login", async (req, res, next) => {
  try {
    const { email, password } = loginSchema.parse(req.body);
    const user = await prisma.user.findUnique({ where: { email: email.toLowerCase() } });
    if (!user || !user.isActive) {
      res.status(401).json({ error: "Invalid credentials" });
      return;
    }
    const ok = await verifyPassword(password, user.passwordHash);
    if (!ok) {
      res.status(401).json({ error: "Invalid credentials" });
      return;
    }
    const accessToken = jwt.sign({ sub: user.id, role: user.role, email: user.email }, ACCESS_SECRET, { expiresIn: "15m" });
    const refreshRaw = crypto.randomBytes(32).toString("hex");
    const refreshHash = hashToken(refreshRaw);
    await prisma.refreshToken.create({ data: { userId: user.id, tokenHash: refreshHash, expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000) } });
    res.json({ accessToken, refreshToken: refreshRaw, user: { id: user.id, email: user.email, name: user.name, role: user.role } });
  } catch (e: any) {
    next(e);
  }
});

router.post("/refresh", async (req, res, next) => {
  try {
    const { refreshToken } = refreshSchema.parse(req.body);
    const hash = hashToken(refreshToken);
    const tokenRow = await prisma.refreshToken.findUnique({ where: { tokenHash: hash }, include: { user: true } });
    if (!tokenRow || tokenRow.revokedAt || tokenRow.expiresAt < new Date()) {
      res.status(401).json({ error: "Invalid refresh token" });
      return;
    }
    if (tokenRow.replacedById) {
      await prisma.refreshToken.updateMany({
        where: { userId: tokenRow.userId, revokedAt: null },
        data: { revokedAt: new Date() },
      });
      res.status(401).json({ error: "Token reuse detected" });
      return;
    }
    if (!tokenRow.user.isActive) {
      res.status(401).json({ error: "Account is inactive" });
      return;
    }
    const newRaw = crypto.randomBytes(32).toString("hex");
    const newHash = hashToken(newRaw);
    const newToken = await prisma.refreshToken.create({ data: { userId: tokenRow.userId, tokenHash: newHash, expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000) } });
    await prisma.refreshToken.update({ where: { id: tokenRow.id }, data: { revokedAt: new Date(), replacedById: newToken.id } });
    const accessToken = jwt.sign({ sub: tokenRow.user.id, role: tokenRow.user.role, email: tokenRow.user.email }, ACCESS_SECRET, { expiresIn: "15m" });
    res.json({ accessToken, refreshToken: newRaw, user: { id: tokenRow.user.id, email: tokenRow.user.email, name: tokenRow.user.name, role: tokenRow.user.role } });
  } catch (e: any) {
    next(e);
  }
});

router.post("/logout", async (req, res, next) => {
  try {
    const auth = req.headers.authorization || "";
    if (!auth.startsWith("Bearer ")) {
      res.status(401).json({ error: "No token" });
      return;
    }
    const tok = auth.slice(7);
    const payload = jwt.verify(tok, ACCESS_SECRET) as any;
    await prisma.refreshToken.updateMany({ where: { userId: payload.sub, revokedAt: null }, data: { revokedAt: new Date() } });
    res.json({ ok: true });
  } catch (e: any) {
    next(e);
  }
});

router.get("/me", async (req, res, next) => {
  try {
    const auth = req.headers.authorization || "";
    if (!auth.startsWith("Bearer ")) {
      res.status(401).json({ error: "No token" });
      return;
    }
    const payload = jwt.verify(auth.slice(7), ACCESS_SECRET) as any;
    const user = await prisma.user.findUnique({ where: { id: payload.sub }, select: { id: true, email: true, name: true, role: true, isActive: true } });
    if (!user || !user.isActive) {
      res.status(401).json({ error: "Account is inactive" });
      return;
    }
    res.json({ user });
  } catch (e: any) {
    next(e);
  }
});

export default router;
