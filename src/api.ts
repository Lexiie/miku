import express from 'express';
import { AgentRuntime } from '@elizaos/core';

export function setupApiRoutes(app: express.Application, runtime: AgentRuntime) {
  app.post('/api/chat', async (req, res) => {
    try {
      const { text, userId } = req.body;
      
      if (!text) {
        return res.status(400).json({ error: 'Text is required' });
      }
      
      // Create message for agent
      const message = {
        content: { text },
        userId: userId || 'android_user',
        roomId: 'android_room'
      };
      
      // Process with agent
      const response = await runtime.processMessage(message);
      
      // Parse response
      let parsedResponse;
      try {
        parsedResponse = JSON.parse(response.text);
      } catch {
        parsedResponse = {
          text: response.text,
          actions: []
        };
      }
      
      res.json(parsedResponse);
    } catch (error) {
      console.error('Chat error:', error);
      res.status(500).json({ 
        text: 'Error processing request',
        actions: []
      });
    }
  });
  
  app.get('/health', (req, res) => {
    res.json({ status: 'ok', timestamp: Date.now() });
  });
}
