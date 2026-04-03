import { Action, HandlerCallback, IAgentRuntime, Memory, State } from "@elizaos/core";

interface AndroidAction {
  type: string;
  params: Record<string, any>;
}

interface AndroidResponse {
  text: string;
  actions: AndroidAction[];
}

export const androidAutomationAction: Action = {
  name: "ANDROID_AUTOMATION",
  similes: ["AUTOMATE", "CONTROL_DEVICE", "EXECUTE_ACTION"],
  description: "Parse natural language commands into Android automation actions",
  
  validate: async (_runtime: IAgentRuntime, _message: Memory) => {
    return true;
  },

  handler: async (
    _runtime: IAgentRuntime,
    message: Memory,
    _state: State,
    _options: any,
    callback: HandlerCallback
  ) => {
    const userText = message.content.text.toLowerCase();
    const response: AndroidResponse = {
      text: "",
      actions: []
    };

    // SET_ALARM
    if (userText.includes("alarm")) {
      const hourMatch = userText.match(/(\d+)\s*(am|pm|:)/i);
      let hour = hourMatch ? parseInt(hourMatch[1]) : 7;
      if (userText.includes("pm") && hour < 12) hour += 12;
      if (userText.includes("am") && hour === 12) hour = 0;
      
      response.actions.push({
        type: "SET_ALARM",
        params: { hour, minute: 0, label: "Alarm" }
      });
      response.text = `⏰ Alarm set for ${hour}:00`;
    }
    
    // SET_TIMER
    else if (userText.includes("timer")) {
      const minuteMatch = userText.match(/(\d+)\s*minute/i);
      const minutes = minuteMatch ? parseInt(minuteMatch[1]) : 5;
      
      response.actions.push({
        type: "SET_TIMER",
        params: { duration: minutes * 60 * 1000 }
      });
      response.text = `⏱️ Timer set for ${minutes} minutes`;
    }
    
    // TOGGLE_WIFI
    else if (userText.includes("wifi")) {
      const enable = userText.includes("on") || userText.includes("enable");
      
      response.actions.push({
        type: "TOGGLE_WIFI",
        params: { enable }
      });
      response.text = `📶 WiFi turned ${enable ? "on" : "off"}`;
    }
    
    // TOGGLE_BLUETOOTH
    else if (userText.includes("bluetooth")) {
      const enable = userText.includes("on") || userText.includes("enable");
      
      response.actions.push({
        type: "TOGGLE_BLUETOOTH",
        params: { enable }
      });
      response.text = `🔵 Bluetooth turned ${enable ? "on" : "off"}`;
    }
    
    // TOGGLE_FLASHLIGHT
    else if (userText.includes("flashlight") || userText.includes("torch")) {
      const enable = userText.includes("on") || userText.includes("enable");
      
      response.actions.push({
        type: "TOGGLE_FLASHLIGHT",
        params: { enable }
      });
      response.text = `🔦 Flashlight turned ${enable ? "on" : "off"}`;
    }
    
    // SET_BRIGHTNESS
    else if (userText.includes("brightness")) {
      const levelMatch = userText.match(/(\d+)%?/);
      const level = levelMatch ? parseInt(levelMatch[1]) : 50;
      
      response.actions.push({
        type: "SET_BRIGHTNESS",
        params: { level }
      });
      response.text = `☀️ Brightness set to ${level}%`;
    }
    
    // SET_VOLUME
    else if (userText.includes("volume")) {
      const levelMatch = userText.match(/(\d+)%?/);
      const level = levelMatch ? parseInt(levelMatch[1]) : 50;
      
      response.actions.push({
        type: "SET_VOLUME",
        params: { level, stream: "music" }
      });
      response.text = `🔊 Volume set to ${level}%`;
    }
    
    // SET_RINGER_MODE
    else if (userText.includes("silent") || userText.includes("vibrate") || userText.includes("ring")) {
      let mode = "normal";
      if (userText.includes("silent")) mode = "silent";
      else if (userText.includes("vibrate")) mode = "vibrate";
      
      response.actions.push({
        type: "SET_RINGER_MODE",
        params: { mode }
      });
      response.text = `🔔 Phone set to ${mode} mode`;
    }
    
    // SEND_SMS
    else if (userText.includes("sms") || userText.includes("text")) {
      const phoneMatch = userText.match(/\d{10,}/);
      const phone = phoneMatch ? phoneMatch[0] : "";
      const messageMatch = userText.match(/saying (.+)/i);
      const messageText = messageMatch ? messageMatch[1] : "Hello";
      
      if (phone) {
        response.actions.push({
          type: "SEND_SMS",
          params: { phone, message: messageText }
        });
        response.text = `📱 SMS sent to ${phone}`;
      } else {
        response.text = "❌ Phone number not found";
      }
    }
    
    // MAKE_CALL
    else if (userText.includes("call")) {
      const phoneMatch = userText.match(/\d{10,}/);
      const phone = phoneMatch ? phoneMatch[0] : "";
      
      if (phone) {
        response.actions.push({
          type: "MAKE_CALL",
          params: { phone }
        });
        response.text = `📞 Calling ${phone}`;
      } else {
        response.text = "❌ Phone number not found";
      }
    }
    
    // OPEN_APP
    else if (userText.includes("open")) {
      const appMatch = userText.match(/open\s+(\w+)/i);
      const appName = appMatch ? appMatch[1] : "";
      
      if (appName) {
        response.actions.push({
          type: "OPEN_APP",
          params: { appName }
        });
        response.text = `📱 Opening ${appName}`;
      } else {
        response.text = "❌ App name not found";
      }
    }
    
    // GET_LOCATION
    else if (userText.includes("location") || userText.includes("where am i")) {
      response.actions.push({
        type: "GET_LOCATION",
        params: {}
      });
      response.text = `📍 Getting your location...`;
    }
    
    // SEND_NOTIFICATION
    else if (userText.includes("notify") || userText.includes("notification")) {
      const messageMatch = userText.match(/notify.*?(?:me|that)\s+(.+)/i);
      const messageText = messageMatch ? messageMatch[1] : "Notification";
      
      response.actions.push({
        type: "SEND_NOTIFICATION",
        params: { title: "Reminder", body: messageText }
      });
      response.text = `🔔 Notification sent`;
    }
    
    // ADD_CALENDAR
    else if (userText.includes("calendar") || userText.includes("meeting") || userText.includes("event")) {
      const titleMatch = userText.match(/(?:add|create)\s+(.+?)\s+(?:at|on|tomorrow)/i);
      const title = titleMatch ? titleMatch[1] : "Event";
      
      response.actions.push({
        type: "ADD_CALENDAR",
        params: { 
          title,
          start: new Date(Date.now() + 86400000).toISOString(),
          end: new Date(Date.now() + 90000000).toISOString()
        }
      });
      response.text = `📅 Event "${title}" added to calendar`;
    }
    
    // GET_EVENTS
    else if (userText.includes("what") && userText.includes("calendar")) {
      response.actions.push({
        type: "GET_EVENTS",
        params: { date: "today" }
      });
      response.text = `📅 Fetching today's events...`;
    }
    
    // Default
    else {
      response.text = "I can help you with: alarms, timers, WiFi, Bluetooth, flashlight, brightness, volume, SMS, calls, apps, location, notifications, and calendar. What would you like to do?";
    }

    if (callback) {
      callback({
        text: JSON.stringify(response),
        content: { text: JSON.stringify(response) }
      });
    }

    return true;
  },

  examples: [
    [
      {
        user: "{{user1}}",
        content: { text: "Set alarm for 7 AM" }
      },
      {
        user: "{{agentName}}",
        content: { text: '{"text":"⏰ Alarm set for 7:00","actions":[{"type":"SET_ALARM","params":{"hour":7,"minute":0,"label":"Alarm"}}]}' }
      }
    ]
  ]
};
