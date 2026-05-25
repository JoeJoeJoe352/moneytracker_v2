import js from "@eslint/js";
import globals from "globals";
import tseslint from "typescript-eslint";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";


export default [
  // Alap JS szabályok
  js.configs.recommended,

  // TypeScript + type-aware linting
  ...tseslint.configs.recommendedTypeChecked,
{
    ignores: [
      "node_modules",
      "eslint.config.js",
      "vite.config.ts"
    ]
  },
  {
    files: ["**/*.{js,jsx,ts,tsx}"],
    
    languageOptions: {
      parser: tseslint.parser,
      parserOptions: {
        project: "./tsconfig.json"
      },
      globals: {
        ...globals.browser
      }
    },

    plugins: {
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh
    },

    rules: {
      // React Hooks szabályok
      "react-hooks/rules-of-hooks": "error",
      "react-hooks/exhaustive-deps": "warn",

      // React Fast Refresh (Vite)
      "react-refresh/only-export-components": "warn"
    }
  }
];