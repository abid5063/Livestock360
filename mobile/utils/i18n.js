import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

// Import translation files
import enTranslations from './translations/en.json';
import bnTranslations from './translations/bn.json';

const resources = {
  en: {
    translation: enTranslations
  },
  bn: {
    translation: bnTranslations
  }
};

i18n
  .use(initReactI18next)
  .init({
    resources,
    lng: 'en', // default language
    fallbackLng: 'en',
    interpolation: {
      escapeValue: false
    },
    react: {
      useSuspense: false
    },
    debug: false,
    keySeparator: '.',
    nsSeparator: ':'
  });

export default i18n; 