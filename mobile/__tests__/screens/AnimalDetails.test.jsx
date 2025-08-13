/* eslint-disable no-undef */
import React from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react-native';
import AnimalDetails from '../../app/animalDetails';
import { router } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from 'axios';

// Mock dependencies
jest.mock('expo-router', () => ({
  router: {
    replace: jest.fn(),
    back: jest.fn(),
  },
  useLocalSearchParams: jest.fn(),
}));

jest.mock('@react-native-async-storage/async-storage', () => ({
  getItem: jest.fn(),
}));

jest.mock('axios');

// Sample animal data for testing
const mockAnimal = {
  _id: '123',
  name: 'Spot',
  type: 'Dog',
  breed: 'Labrador',
  age: 3,
  gender: 'Male',
  details: 'Friendly dog',
  photo_url: 'https://example.com/photo.jpg'
};

describe('AnimalDetails Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    
    // Setup default mocks
    require('expo-router').useLocalSearchParams.mockReturnValue({
      animal: JSON.stringify(mockAnimal)
    });
    AsyncStorage.getItem.mockResolvedValue('mock-token');
    axios.put.mockResolvedValue({ data: { message: 'Animal updated successfully' } });
    axios.delete.mockResolvedValue({ data: { message: 'Animal deleted successfully' } });
  });
  
  // Clean up after all tests
  afterAll(() => {
    jest.restoreAllMocks();
  });

  it('renders animal details correctly', () => {
    const { getByText } = render(<AnimalDetails />);
    
    expect(getByText('Spot')).toBeTruthy();
    expect(getByText('Type: Dog')).toBeTruthy();
    expect(getByText('Breed: Labrador')).toBeTruthy();
    expect(getByText('Age: 3 years')).toBeTruthy();
    expect(getByText('Gender: Male')).toBeTruthy();
    expect(getByText('Details: Friendly dog')).toBeTruthy();
  });

  it('renders no animal data message when animal is null', () => {
    require('expo-router').useLocalSearchParams.mockReturnValue({});
    
    const { getByText } = render(<AnimalDetails />);
    
    expect(getByText('No animal data found.')).toBeTruthy();
    expect(getByText('Go Back')).toBeTruthy();
  });

  it('navigates back when back button is pressed', () => {
    const { getByTestId } = render(<AnimalDetails />);
    
    fireEvent.press(getByTestId('back-button'));
    
    expect(router.back).toHaveBeenCalled();
  });

  it('navigates back to profile from error state', () => {
    require('expo-router').useLocalSearchParams.mockReturnValue({});
    
    const { getByTestId } = render(<AnimalDetails />);
    
    fireEvent.press(getByTestId('error-back-button'));
    
    expect(router.back).toHaveBeenCalled();
  });

  it('opens edit modal when edit button is pressed', () => {
    const { getByText, getByTestId } = render(<AnimalDetails />);
    
    fireEvent.press(getByTestId('animal-edit-button'));
    
    // Modal should be visible with title
    expect(getByText('Edit Animal')).toBeTruthy();
  });

  it('updates form data when input changes', () => {
    const { getByTestId } = render(<AnimalDetails />);
    
    // Open edit modal
    fireEvent.press(getByTestId('animal-edit-button'));
    
    // Change name input
    const nameInput = getByTestId('animal-name-input');
    fireEvent.changeText(nameInput, 'Max');
    
    // Verify input was updated
    expect(nameInput.props.value).toBe('Max');
  });

  it('closes modal when cancel button is pressed', async () => {
    const { getByText, queryByText, getByTestId } = render(<AnimalDetails />);
    
    // Open edit modal
    fireEvent.press(getByTestId('animal-edit-button'));
    expect(getByText('Edit Animal')).toBeTruthy();
    
    // Cancel
    fireEvent.press(getByTestId('animal-cancel-button'));
    
    // Modal title should no longer be visible
    await waitFor(() => {
      expect(queryByText('Edit Animal')).toBeNull();
    });
  });

  it('renders form fields in edit modal', () => {
    const { getByTestId, getByPlaceholderText } = render(<AnimalDetails />);
    
    // Open edit modal
    fireEvent.press(getByTestId('animal-edit-button'));
    
    // Verify form fields are displayed
    expect(getByTestId('animal-name-input')).toBeTruthy();
    expect(getByPlaceholderText('Type')).toBeTruthy();
    expect(getByPlaceholderText('Breed')).toBeTruthy();
    expect(getByPlaceholderText('Age')).toBeTruthy();
    expect(getByPlaceholderText('Gender')).toBeTruthy();
    expect(getByPlaceholderText('Details')).toBeTruthy();
  });

  it('shows loading indicator when submitting form', async () => {
    // Mock a delayed response
    axios.put.mockImplementation(() => new Promise(resolve => setTimeout(() => {
      resolve({ data: { message: 'Animal updated successfully' } });
    }, 100)));
    
    const { getByTestId } = render(<AnimalDetails />);
    
    // Open edit modal
    fireEvent.press(getByTestId('animal-edit-button'));
    
    // Submit form
    fireEvent.press(getByTestId('animal-save-button'));
    
    // Loading indicator should be visible
    expect(getByTestId('animal-loading-indicator')).toBeTruthy();
  });

  it('renders edit and delete buttons', () => {
    const { getByTestId } = render(<AnimalDetails />);
    
    // Verify the buttons exist
    expect(getByTestId('animal-edit-button')).toBeTruthy();
    expect(getByTestId('animal-delete-button')).toBeTruthy();
  });
});
