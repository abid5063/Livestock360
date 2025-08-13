import Translate from 'react-native-translate';

// Configure the translation service
Translate.setConfig({
  from: 'en',
  to: 'bn',
  // You can add API key if needed
  // apiKey: 'YOUR_API_KEY'
});

export const translateWithRNTranslate = async (text, targetLanguage) => {
  try {
    const translatedText = await Translate.translate(text, {
      from: 'en',
      to: targetLanguage
    });
    
    return translatedText;
  } catch (error) {
    console.error('React Native Translate error:', error);
    throw error;
  }
};

// Cache for translations
const rnTranslateCache = new Map();

export const getCachedRNTranslation = async (text, targetLanguage) => {
  const cacheKey = `${text}_${targetLanguage}`;
  
  if (rnTranslateCache.has(cacheKey)) {
    return rnTranslateCache.get(cacheKey);
  }
  
  const translatedText = await translateWithRNTranslate(text, targetLanguage);
  rnTranslateCache.set(cacheKey, translatedText);
  
  return translatedText;
}; 