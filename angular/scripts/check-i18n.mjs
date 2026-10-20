// Fordítási kulcsok ellenőrzése (CI-ban fut, hiba esetén 1-es kóddal lép ki):
//  1. minden nyelvi fájlban ugyanazok a kulcsok szerepelnek, és egyik érték sem üres
//  2. a kódban egyértelműen fordítási kulcsként használt szövegek (_('...'), '...' | translate,
//     instant('...')) léteznek a fordítási fájlokban
//  3. nincs olyan kulcs, ami sehol nem szerepel a forráskódban (halott kulcs)
// Kulcsot nem szabad összefűzéssel előállítani ('wallet.type.' + type), mert a 3. pont csak a teljes
// kulcs szövegét keresi; helyette a teljes kulcs szerepeljen a kódban (pl. egy Record-ban)
import { readdirSync, readFileSync } from 'node:fs';
import { join, relative } from 'node:path';

const ROOT = join(import.meta.dirname, '..');
const I18N_DIR = join(ROOT, 'public', 'i18n');
const SRC_DIR = join(ROOT, 'src');

/**
 * Kulcsok, amelyek nem szerepelnek a frontend forráskódjában, de használatban vannak (pl. a backend küldi)
 */
const IGNORED_UNUSED_KEYS = new Set([]);

const errors = [];

/**
 * A beágyazott json-t lapos "a.b.c" kulcsokká alakítja (a kulcsnevekben is lehet pont, pl. "email.required-error")
 */
function flatten(object, prefix = '', result = new Map()) {
  for (const [key, value] of Object.entries(object)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (value !== null && typeof value === 'object') {
      flatten(value, path, result);
    } else {
      result.set(path, value);
    }
  }
  return result;
}

function listFiles(dir) {
  return readdirSync(dir, { withFileTypes: true, recursive: true })
    .filter((entry) => entry.isFile())
    .map((entry) => join(entry.parentPath, entry.name));
}

// --- 1. nyelvi fájlok összevetése ---
const languages = new Map(
  readdirSync(I18N_DIR)
    .filter((file) => file.endsWith('.json'))
    .map((file) => [
      file.replace('.json', ''),
      flatten(JSON.parse(readFileSync(join(I18N_DIR, file), 'utf8'))),
    ]),
);

const allKeys = new Set([...languages.values()].flatMap((keys) => [...keys.keys()]));

for (const [lang, keys] of languages) {
  for (const key of allKeys) {
    if (!keys.has(key)) {
      errors.push(`[${lang}.json] hiányzó kulcs: ${key}`);
    } else if (typeof keys.get(key) !== 'string' || keys.get(key).trim() === '') {
      errors.push(`[${lang}.json] üres vagy nem szöveg érték: ${key}`);
    }
  }
}

// --- 2. és 3. forráskód átnézése ---
const sourceFiles = listFiles(SRC_DIR).filter(
  (file) =>
    /\.(ts|html)$/.test(file) && !file.endsWith('.spec.ts') && !file.includes('test-support'),
);

const EXPLICIT_KEY_PATTERNS = [
  /\b_\(\s*'([^']+)'\s*\)/g,
  /\binstant\(\s*'([^']+)'/g,
  /'([^']+)'\s*\)?\s*\|\s*translate\b/g,
  // (feltétel ? 'a' : 'b') | translate
  /\?\s*'([^']+)'\s*:\s*'[^']+'\s*\)\s*\|\s*translate\b/g,
];
const STRING_LITERAL_PATTERN = /(['"`])([\w.-]+)\1/g;

const usedLiterals = new Set();

for (const file of sourceFiles) {
  const content = readFileSync(file, 'utf8');
  const location = relative(ROOT, file).replaceAll('\\', '/');

  for (const pattern of EXPLICIT_KEY_PATTERNS) {
    for (const match of content.matchAll(pattern)) {
      if (!allKeys.has(match[1])) {
        const line = content.slice(0, match.index).split('\n').length;
        errors.push(`[${location}:${line}] nem létező fordítási kulcs: ${match[1]}`);
      }
    }
  }

  for (const match of content.matchAll(STRING_LITERAL_PATTERN)) {
    usedLiterals.add(match[2]);
  }
}

for (const key of allKeys) {
  if (!usedLiterals.has(key) && !IGNORED_UNUSED_KEYS.has(key)) {
    errors.push(`nem használt kulcs: ${key}`);
  }
}

if (errors.length > 0) {
  console.error(errors.join('\n'));
  console.error(`\n${errors.length} fordítási hiba`);
  process.exit(1);
}

console.log(`Fordítások rendben (${languages.size} nyelv, ${allKeys.size} kulcs)`);
