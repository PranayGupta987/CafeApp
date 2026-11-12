import { z } from "zod";

export const registerSchema = z.object({
  email: z.string().email("Invalid email"),
  password: z.string().min(8, "Password must be at least 8 characters"),
  name: z.string().min(1, "Name is required"),
  role: z.enum(["CUSTOMER", "CAFE_ADMIN", "CAFE_STAFF", "KITCHEN"]).optional().default("CUSTOMER"),
});

export const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
});

export const refreshSchema = z.object({
  refreshToken: z.string().min(1),
});

export const googleAuthSchema = z.object({
  idToken: z.string().optional(),
  accessToken: z.string().optional(),
  email: z.string().email().optional(),
  name: z.string().optional(),
  googleId: z.string().optional(),
}).refine((data) => data.idToken || data.accessToken || (data.email && data.googleId), {
  message: "Either idToken, accessToken, or verified email and googleId is required",
});

export type GoogleAuthInput = z.infer<typeof googleAuthSchema>;

