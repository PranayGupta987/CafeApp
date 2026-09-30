import { prisma } from "../../lib/prisma.js";
import argon2 from "argon2";
import bcrypt from "bcrypt";
import crypto from "crypto";

const ACCESS_SECRET = process.env.JWT_ACCESS_SECRET || "cafeapp-default-access-secret-32-chars-long";
const REFRESH_SECRET = process.env.JWT_REFRESH_SECRET || "cafeapp-default-refresh-secret-32-chars-long";

async function hashPassword(pw: string): Promise<string> {
  try {
    return await argon2.hash(pw, { type: argon2.argon2id, timeCost: 3, memoryCost: 65536, parallelism: 4 });
  } catch {
    return bcrypt.hashSync(pw, 10); // Windows native binding fallback
  }
}

function verifyPassword(pw: string, hash: string): Promise<boolean> {
  try {
    if (hash.startsWith("$argon2")) return argon2.verify(hash, pw);
    return Promise.resolve(bcrypt.compareSync(pw, hash));
  } catch {
    return Promise.resolve(bcrypt.compareSync(pw, hash));
  }
}

function hashToken(t: string): string {
  return crypto.createHash("sha256").update(t).digest("hex");
}

export { prisma, hashPassword, verifyPassword, hashToken, ACCESS_SECRET, REFRESH_SECRET };
