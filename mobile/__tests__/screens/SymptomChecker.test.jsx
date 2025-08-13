/* eslint-disable no-undef */
import React from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react-native';
import DiseaseDetection from '../../app/symptomChecker';
import * as ImagePicker from 'expo-image-picker';
import { Alert } from 'react-native';

// Mock ImagePicker
jest.mock('expo-image-picker', () => ({
  MediaTypeOptions: {
    Images: 'Images',
  },
  requestMediaLibraryPermissionsAsync: jest.fn(),
  launchImageLibraryAsync: jest.fn(),
}));

// Mock fetch responses
const mockSuccessResponse = {
  json: () => Promise.resolve({
    candidates: [
      {
        content: {
          parts: [
            {
              text: 'The symptoms you described might indicate a respiratory infection. Common respiratory infections in cattle include Bovine Respiratory Disease (BRD) or pneumonia. I recommend consulting a veterinarian for a proper diagnosis and treatment plan.'
            }
          ]
        }
      }
    ]
  })
};

const mockImageSuccessResponse = {
  json: () => Promise.resolve({
    candidates: [
      {
        content: {
          parts: [
            {
              text: 'Based on the image and symptoms provided, this appears to be a case of foot-and-mouth disease. This is a highly contagious viral disease affecting cloven-hoofed animals. Please contact a veterinarian immediately as this is a reportable disease in many countries.'
            }
          ]
        }
      }
    ]
  })
};

describe('DiseaseDetection Component', () => {
  beforeEach(() => {
    // Reset mocks before each test
    global.fetch.mockReset();
    ImagePicker.requestMediaLibraryPermissionsAsync.mockReset();
    ImagePicker.launchImageLibraryAsync.mockReset();
  });

  it('renders correctly with all UI elements', () => {
    const { getByText, getByPlaceholderText } = render(<DiseaseDetection />);
    
    // Check if the header title is displayed
    expect(getByText('Disease Detection')).toBeTruthy();
    
    // Check if the input field is present
    expect(getByPlaceholderText('e.g., coughing, loss of appetite, fever...')).toBeTruthy();
    
    // Check if buttons are present
    expect(getByText('Select Photo (optional)')).toBeTruthy();
    expect(getByText('Analyze')).toBeTruthy();
    expect(getByText('Go Pro')).toBeTruthy();
  });

  it('allows entering symptoms and submits for analysis', async () => {
    global.fetch.mockResolvedValueOnce(mockSuccessResponse);
    
    const { getByText, getByPlaceholderText, getByTestId } = render(<DiseaseDetection />);
    
    // Enter symptoms
    const input = getByTestId('symptoms-input');
    fireEvent.changeText(input, 'Coughing and difficulty breathing');
    
    // Press the analyze button
    const analyzeButton = getByTestId('analyze-button');
    fireEvent.press(analyzeButton);
    
    // Check for loading indicator (implied by button being disabled)
    expect(analyzeButton.props.accessibilityState.disabled).toBe(true);
    
    // Wait for the analysis result
    await waitFor(() => {
      expect(getByTestId('result-container')).toBeTruthy();
    });
    
    // Check if the AI response is displayed
    await waitFor(() => {
      expect(getByTestId('result-text')).toBeTruthy();
      expect(getByText(/respiratory infection/i)).toBeTruthy();
    });
    
    // Verify that fetch was called with the correct URL
    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent'),
      expect.any(Object)
    );
  });

  it('handles image picking and includes image in analysis', async () => {
    // Mock the image picker permission and result
    ImagePicker.requestMediaLibraryPermissionsAsync.mockResolvedValueOnce({ status: 'granted' });
    ImagePicker.launchImageLibraryAsync.mockResolvedValueOnce({
      canceled: false,
      assets: [
        {
          uri: 'file://test-image.jpg',
          type: 'image/jpeg',
          base64: 'R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7', // tiny base64 image
        }
      ]
    });
    
    global.fetch.mockResolvedValueOnce(mockImageSuccessResponse);
    
    const { getByText, getByTestId } = render(<DiseaseDetection />);
    
    // Press the image picker button
    const imagePickerButton = getByTestId('image-picker-button');
    fireEvent.press(imagePickerButton);
    
    // Wait for the image picker to be processed
    await waitFor(() => {
      expect(ImagePicker.launchImageLibraryAsync).toHaveBeenCalled();
    });
    
    // Wait for the change photo text to appear
    await waitFor(() => {
      expect(getByText('Change Photo')).toBeTruthy();
    });
    
    // Press the analyze button
    const analyzeButton = getByTestId('analyze-button');
    fireEvent.press(analyzeButton);
    
    // Wait for the analysis result
    await waitFor(() => {
      expect(getByTestId('result-container')).toBeTruthy();
    });
    
    // Check if the AI response is displayed
    await waitFor(() => {
      expect(getByTestId('result-text')).toBeTruthy();
      expect(getByText(/foot-and-mouth disease/i)).toBeTruthy();
    });
    
    // Verify that fetch was called with the correct URL
    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent'),
      expect.any(Object)
    );
  });

  it('handles API errors gracefully', async () => {
    global.fetch.mockRejectedValueOnce(new Error('Network Error'));
    
    const { getByTestId, findByTestId } = render(<DiseaseDetection />);
    
    // Enter symptoms
    const input = getByTestId('symptoms-input');
    fireEvent.changeText(input, 'Fever and weakness');
    
    // Press the analyze button
    const analyzeButton = getByTestId('analyze-button');
    fireEvent.press(analyzeButton);
    
    // Wait for the error message
    const errorText = await findByTestId('error-text');
    expect(errorText).toBeTruthy();
    expect(errorText.props.children).toMatch(/An error occurred/i);
  });

  it('shows error when attempting to analyze without input', async () => {
    const { getByTestId, findByTestId } = render(<DiseaseDetection />);
    
    // Press the analyze button without entering symptoms or selecting an image
    const analyzeButton = getByTestId('analyze-button');
    fireEvent.press(analyzeButton);
    
    // Wait for the error message
    const errorText = await findByTestId('error-text');
    expect(errorText).toBeTruthy();
    expect(errorText.props.children).toMatch(/Please enter symptoms and\/or select a photo/i);
    
    // Verify that fetch was not called
    expect(global.fetch).not.toHaveBeenCalled();
  });
  
  it('handles denied image picker permissions', async () => {
    // Mock denied permissions
    ImagePicker.requestMediaLibraryPermissionsAsync.mockResolvedValueOnce({ status: 'denied' });
    
    const { getByTestId } = render(<DiseaseDetection />);
    
    // Press the image picker button
    const imagePickerButton = getByTestId('image-picker-button');
    fireEvent.press(imagePickerButton);
    
    // Wait for ImagePicker permissions to be requested
    await waitFor(() => {
      expect(ImagePicker.requestMediaLibraryPermissionsAsync).toHaveBeenCalled();
    });
    
    // Verify that the image picker wasn't launched
    expect(ImagePicker.launchImageLibraryAsync).not.toHaveBeenCalled();
  });

  it('handles canceled image picking', async () => {
    // Mock permissions granted but selection canceled
    ImagePicker.requestMediaLibraryPermissionsAsync.mockResolvedValueOnce({ status: 'granted' });
    ImagePicker.launchImageLibraryAsync.mockResolvedValueOnce({
      canceled: true
    });
    
    const { getByTestId, getByText } = render(<DiseaseDetection />);
    
    // Press the image picker button
    const imagePickerButton = getByTestId('image-picker-button');
    fireEvent.press(imagePickerButton);
    
    // Wait for the image picker to be processed
    await waitFor(() => {
      expect(ImagePicker.launchImageLibraryAsync).toHaveBeenCalled();
    });
    
    // Verify we still have the original text (not "Change Photo")
    expect(getByText('Select Photo (optional)')).toBeTruthy();
  });
});
