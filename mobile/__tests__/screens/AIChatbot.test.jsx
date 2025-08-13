/* eslint-disable no-undef */
import React from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react-native';
import AIChatbot from '../../app/aiChatbot';

// Mock fetch responses
const mockSuccessResponse = {
  json: () => Promise.resolve({
    candidates: [
      {
        content: {
          parts: [
            {
              text: 'This is a mock response from the AI.'
            }
          ]
        }
      }
    ]
  })
};

describe('AIChatbot Component', () => {
  beforeEach(() => {
    // Reset the mock before each test
    global.fetch.mockReset();
  });

  it('renders correctly with initial greeting message', () => {
    const { getByText, getByPlaceholderText } = render(<AIChatbot />);
    
    // Check if the initial bot message is displayed
    expect(getByText('Hello! I am your AI assistant. How can I help you today?')).toBeTruthy();
    
    // Check if the input field is present
    expect(getByPlaceholderText('Type your message...')).toBeTruthy();
  });

  it('sends a message and receives a response successfully', async () => {
    global.fetch.mockResolvedValueOnce(mockSuccessResponse);
    
    const { getByText, getByPlaceholderText, getByTestId } = render(<AIChatbot />);
    
    // Type a message in the input field
    const input = getByPlaceholderText('Type your message...');
    fireEvent.changeText(input, 'What are common diseases in cattle?');
    
    // Press the send button
    const sendButton = getByTestId('send-button');
    fireEvent.press(sendButton);
    
    // Check if the user message appears
    expect(getByText('What are common diseases in cattle?')).toBeTruthy();
    
    // Wait for the API response to be processed
    await waitFor(() => {
      expect(getByText('This is a mock response from the AI.')).toBeTruthy();
    });
    
    // Verify that fetch was called with the correct URL
    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent'),
      expect.any(Object)
    );
  });

  it('handles API errors gracefully', async () => {
    global.fetch.mockRejectedValueOnce(new Error('API Error'));
    
    const { getByText, getByPlaceholderText, getByTestId } = render(<AIChatbot />);
    
    // Type a message in the input field
    const input = getByPlaceholderText('Type your message...');
    fireEvent.changeText(input, 'Hello');
    
    // Press the send button
    const sendButton = getByTestId('send-button');
    fireEvent.press(sendButton);
    
    // Check if the user message appears
    expect(getByText('Hello')).toBeTruthy();
    
    // Wait for the error message to appear
    await waitFor(() => {
      expect(getByText('Sorry, I could not get a response. Please try again.')).toBeTruthy();
    });
  });

  it('disables input and shows activity indicator while loading', async () => {
    // Setup a delayed API response to test the loading state
    global.fetch.mockImplementationOnce(() => {
      return new Promise(resolve => {
        setTimeout(() => resolve(mockSuccessResponse), 100);
      });
    });
    
    const { getByPlaceholderText, getByTestId } = render(<AIChatbot />);
    
    // Type and send a message
    const input = getByPlaceholderText('Type your message...');
    fireEvent.changeText(input, 'Test message');
    
    const sendButton = getByTestId('send-button');
    fireEvent.press(sendButton);
    
    // Check if the input is disabled during loading
    expect(input.props.editable).toBe(false);
    
    // Wait for the response to complete
    await waitFor(() => 
      expect(getByPlaceholderText('Type your message...').props.editable).toBe(true),
      { timeout: 500 }
    );
  });
});
