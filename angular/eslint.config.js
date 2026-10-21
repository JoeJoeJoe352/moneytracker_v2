// @ts-check
const eslint = require('@eslint/js');
const { defineConfig } = require('eslint/config');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');

module.exports = defineConfig([
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommendedTypeChecked,
      tseslint.configs.stylisticTypeChecked,
      angular.configs.tsRecommended,
    ],
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: __dirname,
      },
    },
    processor: angular.processInlineTemplates,
    rules: {
      //"@angular-eslint/template/click-events-have-key-events": "off",
      //"@angular-eslint/template/interactive-supports-focus": "off",
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: 'app',
          style: 'camelCase',
        },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: 'app',
          style: 'kebab-case',
        },
      ],
      // az Angular Validators.required/email stb. statikus metódusok, nem használják a this-t
      '@typescript-eslint/unbound-method': 'off',
      // boolean értékeknél a || a helyes operátor (a ?? false esetén nem nézné a jobb oldalt)
      '@typescript-eslint/prefer-nullish-coalescing': [
        'error',
        { ignorePrimitives: { boolean: true } },
      ],
      // tesztekben a protected/private tagokat component['tag'] formában érjük el
      '@typescript-eslint/dot-notation': [
        'error',
        { allowPrivateClassPropertyAccess: true, allowProtectedClassPropertyAccess: true },
      ],
    },
  },
  {
    // a vi.fn() mockok és a TestBed segédfüggvények any típusúak, ezek a szabályok tesztekben csak zajt adnának
    files: ['**/*.spec.ts', 'src/app/test-support/**/*.ts'],
    rules: {
      '@typescript-eslint/no-unsafe-assignment': 'off',
      '@typescript-eslint/no-unsafe-member-access': 'off',
      '@typescript-eslint/no-unsafe-call': 'off',
      '@typescript-eslint/no-unsafe-return': 'off',
      '@typescript-eslint/no-unsafe-argument': 'off',
    },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
    rules: {},
  },
]);
