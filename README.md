# OpenRouter AI Chatbot

A professional Java desktop chatbot application with multi-model OpenRouter integration and built-in NLP analysis. This project showcases:

- Multi-model AI support via OpenRouter
- Java Swing GUI for desktop chat interaction
- Conversation history with context-aware dialogue
- NLP processing: intent detection, sentiment analysis, and keyword extraction
- Secure environment-based API key configuration
- Comprehensive project documentation and usage guides

## Project Overview

This application is designed as a portfolio-ready AI chatbot built in Java. It uses the OpenRouter API to support multiple large language models, including GPT-4o-mini, Claude 3.5 Sonnet, Meta Llama 3.1 70B, and Mistral 7B Instruct.

The interface is implemented with Swing and includes a model selector, chat display, user input field, and a responsive send button.

## Features

- Multi-model AI selection
- Natural language processing analysis
- Real-time chat messages
- Non-blocking API calls for responsive UI
- Error handling and user feedback

## Setup Instructions

1. Create an API key at: https://openrouter.ai/keys
2. Set the environment variable in Windows:

   ```powershell
   setx OPENROUTER_API_KEY "<YOUR_OPENROUTER_API_KEY>"
   ```

3. Open a new terminal and navigate to the project folder:

   ```powershell
   cd "C:\Users\SRINATH M\Desktop\sd\LAB 8"
   ```

4. Compile the source:

   ```powershell
   javac Chat.java
   ```

5. Run the application:

   ```powershell
   java Chat
   ```

## Notes

- Do not store your actual API key in the repository.
- Use the environment variable `OPENROUTER_API_KEY` for secure access.
- The sample documents and quick start guides are included for reference.

## Repository Contents

- `Chat.java` — main application source code
- `COMMAND_REFERENCE.txt` — usage and command reference
- `OPENROUTER_SETUP.txt` — setup instructions for the OpenRouter API
- `PROJECT_COMPLETION_REPORT.txt` — final project summary
- `PROJECT_SUMMARY.txt` — detailed technical summary
- `QUICK_START.txt` — quick start instructions
- `.gitignore` — repository ignore rules
