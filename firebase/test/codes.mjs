// The same code maths as the Android app (SessionCodes.kt) and the rules (codeMatches / backupCodeMatches).
import { createHash } from 'node:crypto';

export const WINDOW_MS = 30_000;
export const windowAt = (ms) => Math.floor(ms / WINDOW_MS);
const sha256Hex = (text) => createHash('sha256').update(text).digest('hex');

export const qrSig = (secret, eventId, stage, window) => sha256Hex(`${secret}|${eventId}|${stage}|${window}`);

export function backupCode(secret, eventId, stage, window) {
  const digits = sha256Hex(`${secret}|code|${eventId}|${stage}|${window}`).replace(/[a-f]/g, (c) => String(c.charCodeAt(0) - 97));
  return digits.slice(0, 6);
}
