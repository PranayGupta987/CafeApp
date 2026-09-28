import pino from 'pino';
import { getEnv } from '../config/env.js';

let _logger: pino.Logger | null = null;

export function getLogger(): pino.Logger {
  if (!_logger) {
    _logger = pino({
      level: getEnv().LOG_LEVEL,
      transport:
        process.env.NODE_ENV !== 'production'
          ? { target: 'pino-pretty', options: { colorize: true } }
          : undefined,
    });
  }
  return _logger;
}

export const logger = new Proxy({} as pino.Logger, {
  get(_, prop) {
    return (getLogger() as any)[prop];
  },
});
