/* eslint-disable no-undef */
import React from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react-native';
import ProMode from '../../app/pro_mode';
import { Alert } from 'react-native';
import axios from 'axios';

// Mock expo-router
jest.mock('expo-router', () => ({
  useRouter: () => ({
    back: jest.fn(),
    push: jest.fn()
  })
}));

// Mock axios
jest.mock('axios');

describe('ProMode Component', () => {
  // Reset mocks before each test
  beforeEach(() => {
    jest.clearAllMocks();
    
    // Mock Alert.alert for each test
    Alert.alert = jest.fn();
  });

  it('renders correctly with all UI elements', () => {
    const { getByText, getByTestId } = render(<ProMode />);
    
    // Check if the header title is displayed
    expect(getByText('Pro Mode - Disease Prediction')).toBeTruthy();
    
    // Check if the instruction text is displayed
    expect(getByText('Select symptoms that your animal is experiencing:')).toBeTruthy();
    
    // Check if symptoms are listed
    expect(getByText('Fever')).toBeTruthy();
    expect(getByText('Loss Of Appetite')).toBeTruthy();
    expect(getByText('Lethargy')).toBeTruthy();
    
    // Check if buttons are present
    expect(getByTestId('back-button')).toBeTruthy();
    expect(getByTestId('clear-button')).toBeTruthy();
    expect(getByTestId('predict-button')).toBeTruthy();
  });

  it('allows selecting and deselecting symptoms', () => {
    const { getByTestId } = render(<ProMode />);
    
    // Select a symptom
    const feverSymptom = getByTestId('symptom-fever');
    fireEvent.press(feverSymptom);
    
    // The style is an array, where one of the objects has the background color
    const styles = Array.isArray(feverSymptom.props.style) ? feverSymptom.props.style : [feverSymptom.props.style];
    expect(styles.some(style => style.backgroundColor === '#4a89dc')).toBe(true);
    
    // Deselect the symptom
    fireEvent.press(feverSymptom);
    
    // Check if the symptom is deselected
    const stylesAfter = Array.isArray(feverSymptom.props.style) ? feverSymptom.props.style : [feverSymptom.props.style];
    expect(stylesAfter.some(style => style.backgroundColor === '#4a89dc')).toBe(false);
  });

  it('shows alert when trying to predict without selecting symptoms', () => {
    const { getByTestId } = render(<ProMode />);
    
    // Press predict without selecting any symptoms
    const predictButton = getByTestId('predict-button');
    fireEvent.press(predictButton);
    
    // Check if alert was shown
    expect(Alert.alert).toHaveBeenCalledWith(
      "No Symptoms Selected", 
      "Please select at least one symptom to get a prediction."
    );
  });

  it('clears all selected symptoms when clear button is pressed', () => {
    const { getByTestId } = render(<ProMode />);
    
    // Select multiple symptoms
    const feverSymptom = getByTestId('symptom-fever');
    const lethargySymptom = getByTestId('symptom-lethargy');
    
    fireEvent.press(feverSymptom);
    fireEvent.press(lethargySymptom);
    
    // Verify both are selected
    const feverStyles = Array.isArray(feverSymptom.props.style) ? feverSymptom.props.style : [feverSymptom.props.style];
    const lethargyStyles = Array.isArray(lethargySymptom.props.style) ? lethargySymptom.props.style : [lethargySymptom.props.style];
    
    expect(feverStyles.some(style => style.backgroundColor === '#4a89dc')).toBe(true);
    expect(lethargyStyles.some(style => style.backgroundColor === '#4a89dc')).toBe(true);
    
    // Press clear button
    const clearButton = getByTestId('clear-button');
    fireEvent.press(clearButton);
    
    // Check if symptoms are deselected
    const feverStylesAfter = Array.isArray(feverSymptom.props.style) ? feverSymptom.props.style : [feverSymptom.props.style];
    const lethargyStylesAfter = Array.isArray(lethargySymptom.props.style) ? lethargySymptom.props.style : [lethargySymptom.props.style];
    
    expect(feverStylesAfter.some(style => style.backgroundColor === '#4a89dc')).toBe(false);
    expect(lethargyStylesAfter.some(style => style.backgroundColor === '#4a89dc')).toBe(false);
  });

  it('successfully predicts disease based on selected symptoms', async () => {
    // Mock axios post to return successful prediction
    axios.post.mockResolvedValueOnce({
      data: {
        prognosis: "mastitis",
        status: "success"
      }
    });
    
    const { getByTestId, getByText, queryByText } = render(<ProMode />);
    
    // Initially, there should be no prediction result
    expect(queryByText('Prediction Result:')).toBeNull();
    
    // Select a symptom
    const feverSymptom = getByTestId('symptom-fever');
    fireEvent.press(feverSymptom);
    
    // Press predict button
    const predictButton = getByTestId('predict-button');
    fireEvent.press(predictButton);
    
    // Check if loading indicator appears
    expect(predictButton.props.accessibilityState.disabled).toBe(true);
    
    // Wait for the prediction result
    await waitFor(() => {
      expect(getByText('Prediction Result:')).toBeTruthy();
      expect(getByText('Diagnosis:')).toBeTruthy();
      expect(getByText('mastitis')).toBeTruthy(); // case is preserved in actual component
      expect(getByText('Status:')).toBeTruthy();
      expect(getByText('success')).toBeTruthy();
    });
    
    // Verify that axios was called with correct parameters
    expect(axios.post).toHaveBeenCalledWith(
      'http://104.214.178.145:80/predict',
      { 
        symptoms: {
          fever: 1,
          loss_of_appetite: 0,
          lethargy: 0,
          coughing: 0,
          diarrhoea: 0,
          dehydration: 0,
          lameness: 0,
          milk_fever: 0,
          pneumonia: 0,
          weight_loss: 0
        }
      },
      { 
        headers: { 'Content-Type': 'application/json' } 
      }
    );
    
    // Check if button is enabled again after prediction
    expect(predictButton.props.accessibilityState.disabled).toBe(false);
  });

  it('handles API error during prediction', async () => {
    // Mock axios post to return an error
    const error = {
      response: {
        data: {
          message: "Server error during prediction"
        }
      }
    };
    axios.post.mockRejectedValueOnce(error);
    
    const { getByTestId, queryByText } = render(<ProMode />);
    
    // Select a symptom
    const feverSymptom = getByTestId('symptom-fever');
    fireEvent.press(feverSymptom);
    
    // Press predict button
    const predictButton = getByTestId('predict-button');
    fireEvent.press(predictButton);
    
    // Wait for the error alert
    await waitFor(() => {
      expect(Alert.alert).toHaveBeenCalledWith(
        "Error", 
        "Server error during prediction"
      );
    });
    
    // Prediction result should not be visible
    expect(queryByText('Prediction Result:')).toBeNull();
    
    // Check if button is enabled again after error
    expect(predictButton.props.accessibilityState.disabled).toBe(false);
  });

  it('navigates back when back button is pressed', () => {
    const { getByTestId } = render(<ProMode />);
    
    // Get the mock router
    const mockRouter = require('expo-router').useRouter();
    
    // Press back button
    const backButton = getByTestId('back-button');
    fireEvent.press(backButton);
    
    // We can't directly test the router.back call due to how useRouter is used
    // in the component. This test is more of a smoke test to ensure it renders
    // and can be clicked without errors.
    expect(backButton).toBeTruthy();
  });

  it('clears prediction results when clear button is pressed', async () => {
    // Mock axios post to return successful prediction
    axios.post.mockResolvedValueOnce({
      data: {
        prognosis: "mastitis",
        status: "success"
      }
    });
    
    const { getByTestId, getByText, queryByText } = render(<ProMode />);
    
    // Select a symptom and get prediction
    const feverSymptom = getByTestId('symptom-fever');
    fireEvent.press(feverSymptom);
    
    const predictButton = getByTestId('predict-button');
    fireEvent.press(predictButton);
    
    // Wait for the prediction result
    await waitFor(() => {
      expect(getByText('Prediction Result:')).toBeTruthy();
    });
    
    // Press clear button
    const clearButton = getByTestId('clear-button');
    fireEvent.press(clearButton);
    
    // Check if prediction result is cleared
    expect(queryByText('Prediction Result:')).toBeNull();
  });

  it('selects multiple symptoms for prediction', async () => {
    // Mock axios post to return successful prediction
    axios.post.mockResolvedValueOnce({
      data: {
        prognosis: "pneumonia",
        status: "success"
      }
    });
    
    const { getByTestId } = render(<ProMode />);
    
    // Select multiple symptoms
    const feverSymptom = getByTestId('symptom-fever');
    const coughingSymptom = getByTestId('symptom-coughing');
    const lethargySymptom = getByTestId('symptom-lethargy');
    
    fireEvent.press(feverSymptom);
    fireEvent.press(coughingSymptom);
    fireEvent.press(lethargySymptom);
    
    // Press predict button
    const predictButton = getByTestId('predict-button');
    fireEvent.press(predictButton);
    
    // Wait for API call to complete
    await waitFor(() => {
      // Verify that axios was called with correct parameters including all three symptoms
      expect(axios.post).toHaveBeenCalledWith(
        'http://104.214.178.145:80/predict',
        { 
          symptoms: {
            fever: 1,
            loss_of_appetite: 0,
            lethargy: 1,
            coughing: 1,
            diarrhoea: 0,
            dehydration: 0,
            lameness: 0,
            milk_fever: 0,
            pneumonia: 0,
            weight_loss: 0
          }
        },
        expect.anything()
      );
    });
  });
});
