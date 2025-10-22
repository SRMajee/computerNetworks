/**
 * DNS Resolver Backend
 * --------------------
 * This server listens for frontend requests,
 * sends a DNS query using UDP sockets to Google's DNS server (8.8.8.8),
 * and returns the resolved IP addresses.
 */

import express from "express";
import cors from "cors";
import dgram from "dgram";
import dnsPacket from "dns-packet";

const app = express();
app.use(cors());

// API: /resolve?domain=example.com
app.get("/resolve", (req, res) => {
  const domain = req.query.domain;
  if (!domain) return res.status(400).json({ error: "Domain is required" });

  // Create DNS query packet
  const message = dnsPacket.encode({
    type: "query",
    id: 1,
    flags: dnsPacket.RECURSION_DESIRED,
    questions: [{ type: "A", name: domain }]
  });

  const socket = dgram.createSocket("udp4");
  const dnsServer = "8.8.8.8";
  const port = 53;
  const startTime = Date.now();

  // Send DNS query
  socket.send(message, port, dnsServer, (err) => {
    if (err) {
      res.status(500).json({ error: "Failed to send DNS query" });
      socket.close();
    }
  });

  // Receive DNS response
  socket.on("message", (response) => {
    const decoded = dnsPacket.decode(response);
    const answers = decoded.answers
      .filter((a) => a.type === "A")
      .map((a) => a.data);
    const duration = Date.now() - startTime;

    res.json({ domain, ips: answers, time: duration });
    socket.close();
  });
});

app.listen(3000, () => {
  console.log("✅ DNS Resolver backend running on port 3000");
  console.log("Try: http://localhost:3000/resolve?domain=google.com");
});
