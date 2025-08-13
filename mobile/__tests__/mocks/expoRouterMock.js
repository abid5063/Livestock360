export const mockExpoRouter = {
  useRouter: jest.fn(() => ({
    back: jest.fn(),
    push: jest.fn(),
    replace: jest.fn(),
    navigate: jest.fn()
  })),
  useLocalSearchParams: jest.fn(() => ({})),
  useFocusEffect: jest.fn((callback) => {
    // Simply execute the callback for testing
    if (typeof callback === 'function') {
      callback();
    }
  }),
  router: {
    back: jest.fn(),
    push: jest.fn(),
    replace: jest.fn(),
    navigate: jest.fn()
  },
  Link: ({ children }) => children
};
