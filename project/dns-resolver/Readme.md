# DNS Resolver using UDP Sockets (Node.js)

## 🎯 Project Overview
This project implements a basic **DNS protocol** using **UDP sockets** in Node.js.  
A simple web interface allows users to input a domain name, and the system queries a real DNS server (Google's 8.8.8.8) to fetch its IP address.

---

## 🧩 Features
- Real DNS queries via UDP
- Returns one or multiple IP addresses
- Displays time taken to resolve
- Simple frontend (HTML + JS)
- Clear button and resolving animation
- Works entirely locally (no React needed)

---

## ⚙️ Setup Instructions

### 1. Install Backend Dependencies
```bash
cd backend
npm install
npm start
```

---

## 📡 Theory Behind DNS Protocol

### 🧠 What is DNS?

**DNS (Domain Name System)** is a hierarchical system that translates human-readable domain names (like `google.com`) into IP addresses (like `142.250.183.206`).  
It works much like a **phonebook for the Internet**, allowing users to access websites using names instead of remembering numerical IPs.

---

### ⚙️ How DNS Works

1. A **client** sends a DNS query to a DNS server.
2. The **DNS server** looks up the IP address associated with the domain.
3. The server **responds** with the resolved IP address.
4. DNS typically operates over **UDP port 53**, though sometimes TCP is used for larger responses.

---

### 🧩 Working of UDP in This Project

**UDP (User Datagram Protocol)** is a **connectionless** transport protocol.  
Unlike TCP, it doesn’t establish a handshake — packets are sent directly without prior communication setup.

In this project:
- The **Node.js backend** creates a UDP socket using:
  ```js
  const socket = dgram.createSocket('udp4');
- The **backend** sends a **binary-encoded DNS query** to Google’s public DNS server at **`8.8.8.8:53`**.  

- When the **DNS server responds**, the **UDP socket** receives the packet.  

- The **`dns-packet`** library decodes the **binary DNS response**.  

- Finally, the **decoded result** (IP address, domain info, etc.) is **sent back to the frontend as JSON**.  
