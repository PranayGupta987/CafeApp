import { z } from 'zod';

const envSchema = z.object({
  NODE_ENV: z.enum(['development', 'production', 'test']).default('production'),
  PORT: z.coerce.number().default(10000),
  DATABASE_URL: z.string().default('postgresql://cafe:cafe@localhost:5432/cafeapp'),
  JWT_ACCESS_SECRET: z.string().default('cafeapp-production-jwt-access-secret-32-chars-minimum'),
  JWT_REFRESH_SECRET: z.string().default('cafeapp-production-jwt-refresh-secret-32-chars-minimum'),
  JWT_ACCESS_TTL: z.string().default('15m'),
  JWT_REFRESH_TTL: z.string().default('30d'),
  CORS_ORIGINS: z.string().default('*'),
  LOG_LEVEL: z.string().default('info'),
  TZ: z.string().default('Asia/Kolkata'),
  QR_TOKEN_BYTES: z.coerce.number().default(32),
  QR_BASE_URL: z.string().default('https://cafeapp.local/t/'),
});

export type Env = z.infer<typeof envSchema>;

let _env: Env | null = null;

export function getEnv(): Env {
  if (!_env) {
    const result = envSchema.safeParse(process.env);
    if (!result.success) {
      console.error('❌ Invalid environment variables:', JSON.stringify(result.error.format()));
      // Fallback to defaults instead of exiting
      _env = envSchema.parse({});
    } else {
      _env = result.data;
    }
  }
  return _env;
}
