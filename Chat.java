import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * OpenRouter Chatbot with NLP Processing
 * Uses OpenRouter API to integrate multiple AI models
 * Includes basic NLP text processing for intent detection
 */
public class Chat extends JFrame {

    private final JTextArea chatArea;
    private final JTextField inputField;
    private final JButton sendButton;
    private final JComboBox<String> modelSelector;
    private String apiKey;
    private OpenRouterClient openRouterClient;
    private NLPProcessor nlpProcessor;
    private List<Message> conversationHistory;

    private static final String ENV_API_KEY = System.getenv("<YOUR_OPENROUTER_API_KEY>");

    public Chat() {
        setTitle("OpenRouter AI Chatbot with NLP");
        setSize(700, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        conversationHistory = new ArrayList<>();
        nlpProcessor = new NLPProcessor();

        // Top panel with model selector
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Model:"));
        modelSelector = new JComboBox<>(new String[]{
            "openai/gpt-4o-mini",
            "anthropic/claude-3.5-sonnet",
            "meta-llama/llama-3.1-70b-instruct",
            "mistralai/mistral-7b-instruct"
        });
        modelSelector.setSelectedIndex(0);
        topPanel.add(modelSelector);
        add(topPanel, BorderLayout.NORTH);

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Bottom panel with input and send button
        JPanel panel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");

        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        add(panel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        chatArea.append("OpenRouter AI Chatbot with NLP Processing\n");
        chatArea.append("=========================================\n\n");
        setVisible(true);

        // Prompt for API key if not set in environment
        promptForApiKey();
    }

    private void promptForApiKey() {
        // First try to use environment variable
        if (ENV_API_KEY != null && !ENV_API_KEY.trim().isEmpty()) {
            apiKey = ENV_API_KEY;
            openRouterClient = new OpenRouterClient(apiKey);
            chatArea.append("✓ API Key loaded from environment variable.\n");
            chatArea.append("✓ Ready to chat!\n\n");
            return;
        }

        // If no environment variable, prompt user
        String input = JOptionPane.showInputDialog(
            this,
            "Enter your OpenRouter API Key:\n(Get one from https://openrouter.ai/keys)",
            "OpenRouter API Key Required",
            JOptionPane.QUESTION_MESSAGE
        );

        if (input == null || input.trim().isEmpty()) {
            chatArea.append("✗ No API key provided. Please restart and enter your API key.\n\n");
            apiKey = null;
            return;
        }

        apiKey = input.trim();
        openRouterClient = new OpenRouterClient(apiKey);
        chatArea.append("✓ API Key configured successfully.\n");
        chatArea.append("✓ Ready to chat!\n\n");
    }

    private void sendMessage() {
        String userInput = inputField.getText().trim();
        if (userInput.isEmpty() || apiKey == null) {
            if (apiKey == null) {
                chatArea.append("System: API Key not configured!\n\n");
            }
            return;
        }

        // NLP Processing: Analyze input
        NLPAnalysis analysis = nlpProcessor.analyzeText(userInput);

        chatArea.append("You: " + userInput + "\n");
        if (!analysis.getKeywords().isEmpty()) {
            chatArea.append("[NLP Keywords: " + analysis.getKeywords() + "]\n");
        }
        inputField.setText("");

        // Add to conversation history
        Message userMessage = new Message("user", userInput);
        conversationHistory.add(userMessage);

        // Send to OpenRouter in background thread
        new Thread(() -> {
            String response = getOpenRouterResponse(userInput);
            SwingUtilities.invokeLater(() -> {
                chatArea.append("Assistant: " + response + "\n\n");
                // Add assistant response to history
                Message assistantMessage = new Message("assistant", response);
                conversationHistory.add(assistantMessage);
            });
        }).start();
    }

    private String getOpenRouterResponse(String userMessage) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "API Key not configured! Please restart the application.";
        }

        try {
            String selectedModel = (String) modelSelector.getSelectedItem();
            return openRouterClient.sendMessage(conversationHistory, userMessage, selectedModel);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Chat::new);
    }
}

/**
 * OpenRouter API Client
 */
class OpenRouterClient {
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private final String apiKey;

    public OpenRouterClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public String sendMessage(List<Message> history, String userMessage, String model) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(API_URL).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("HTTP-Referer", "http://localhost");
            conn.setRequestProperty("X-Title", "OpenRouter Chatbot");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            // Build message array
            StringBuilder messagesJson = new StringBuilder("[");
            for (int i = 0; i < history.size(); i++) {
                Message msg = history.get(i);
                if (i > 0) messagesJson.append(",");
                messagesJson.append("{\"role\":\"").append(msg.getRole()).append("\",");
                messagesJson.append("\"content\":\"").append(escapeJson(msg.getContent())).append("\"}");
            }
            // Add current user message
            messagesJson.append(",{\"role\":\"user\",\"content\":\"").append(escapeJson(userMessage)).append("\"}]");

            String jsonPayload = "{"
                + "\"model\":\"" + model + "\","
                + "\"messages\":" + messagesJson.toString() + ","
                + "\"temperature\":0.7,"
                + "\"top_p\":0.9,"
                + "\"max_tokens\":1000"
                + "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int status = conn.getResponseCode();
            StringBuilder response = new StringBuilder();

            BufferedReader br;
            if (status >= 200 && status < 300) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            if (status < 200 || status >= 300) {
                return "API Error (Status " + status + "): " + response.toString();
            }

            // Parse response to extract text
            return parseOpenRouterResponse(response.toString());

        } catch (Exception e) {
            return "Connection Error: " + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String parseOpenRouterResponse(String responseJson) {
        try {
            // Extract text from: {"choices":[{"message":{"content":"text here"}}]}
            String searchKey = "\"content\":\"";
            int start = responseJson.indexOf(searchKey);
            if (start != -1) {
                start += searchKey.length();
                int end = start;
                while (end < responseJson.length()) {
                    if (responseJson.charAt(end) == '"' && 
                        (end == 0 || responseJson.charAt(end - 1) != '\\')) {
                        break;
                    }
                    end++;
                }
                if (end > start && end <= responseJson.length()) {
                    String text = responseJson.substring(start, end);
                    return text.replace("\\n", "\n")
                              .replace("\\\"", "\"")
                              .replace("\\\\", "\\");
                }
            }
            return "Could not parse API response.";
        } catch (Exception e) {
            return "Parse Error: " + e.getMessage();
        }
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}

/**
 * Message class for conversation history
 */
class Message {
    private final String role;
    private final String content;

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
}

/**
 * NLP Processor for text analysis
 */
class NLPProcessor {
    private static final String[] KEYWORDS = {
        "hello", "hi", "hey", "thank", "thanks", "yes", "no", "help", "please",
        "what", "why", "how", "when", "where", "who", "which", "weather", "time"
    };

    public NLPAnalysis analyzeText(String text) {
        NLPAnalysis analysis = new NLPAnalysis(text);

        // Convert to lowercase for keyword detection
        String lowerText = text.toLowerCase();

        // Extract keywords
        for (String keyword : KEYWORDS) {
            if (lowerText.contains(keyword)) {
                analysis.addKeyword(keyword);
            }
        }

        // Detect intent
        if (lowerText.contains("hello") || lowerText.contains("hi") || lowerText.contains("hey")) {
            analysis.setIntent("greeting");
        } else if (lowerText.contains("help") || lowerText.contains("how")) {
            analysis.setIntent("help_request");
        } else if (lowerText.contains("thank")) {
            analysis.setIntent("gratitude");
        } else if (lowerText.contains("?")) {
            analysis.setIntent("question");
        } else {
            analysis.setIntent("statement");
        }

        // Calculate sentiment polarity (simple: positive/negative/neutral)
        if (lowerText.contains("good") || lowerText.contains("great") || lowerText.contains("excellent")) {
            analysis.setSentiment("positive");
        } else if (lowerText.contains("bad") || lowerText.contains("poor") || lowerText.contains("terrible")) {
            analysis.setSentiment("negative");
        } else {
            analysis.setSentiment("neutral");
        }

        return analysis;
    }
}

/**
 * NLP Analysis Result
 */
class NLPAnalysis {
    private final String originalText;
    private final List<String> keywords;
    private String intent;
    private String sentiment;

    public NLPAnalysis(String text) {
        this.originalText = text;
        this.keywords = new ArrayList<>();
        this.intent = "unknown";
        this.sentiment = "neutral";
    }

    public void addKeyword(String keyword) {
        if (!keywords.contains(keyword)) {
            keywords.add(keyword);
        }
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getIntent() {
        return intent;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getSentiment() {
        return sentiment;
    }
}
