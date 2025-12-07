# PeerPass

![PeerPass Status](https://img.shields.io/badge/Status-Live-success)
![Backend](https://img.shields.io/badge/Backend-Java_Sockets-orange)
![Frontend](https://img.shields.io/badge/Frontend-Next.js-black)

**PeerPass** is a lightweight peer-to-peer file-sharing platform designed to demonstrate low-level networking concepts. It combines a raw Java socket backend with a modern Next.js frontend to facilitate ephemeral file sharing.

🔗 **Live Demo:** [https://peerpass.vercel.app/](https://peerpass.vercel.app/)

---

## 🚀 How It Works

PeerPass moves away from traditional database storage for file transfers, utilizing direct socket streams and dedicated threads instead.

1. **Upload:** A user uploads a file via the frontend.
2. **Dynamic Port Assignment:** The Java server receives the stream via `multipart/form-data` and assigns a unique, dynamic download port.
3. **Thread Spawning:** The server spawns a dedicated thread responsible solely for serving that specific file.
4. **Download:** Any user can download the file by connecting to the specific port (e.g., `/download/<port>`), streaming the data over raw sockets.

---

## 🛠 Tech Stack

### Backend (Hosted on Railway)
* **Language:** Java
* **Core Concepts:** Raw Sockets, Input/Output Streams, Multithreading
* **Architecture:** Custom HTTP Parser (No frameworks like Spring or Javalin)

### Frontend (Hosted on Vercel)
* **Framework:** Next.js
* **Styling:** CSS
* **Components:** Custom React hooks for file handling

---

## 📂 Project Structure

```text
/peerpass
├── /server                 # Java Backend
│   ├── Main.java           # Entry point
│   ├── FileController.java # Routes traffic
│   ├── UploadHandler.java  # Handles multipart/form-data
│   ├── DownloadHandler.java# Manages socket streaming
│   ├── FileSharer.java     # Logic for file availability
│   ├── Multiparser.java    # Custom multipart parser
│   └── utils/
│       ├── StreamUtils.java
│       └── HttpUtils.java
│
└── /client                 # Next.js Frontend
    ├── /app
    │   ├── page.tsx        # Main UI
    │   └── globals.css     # Styles
    ├── /components
    │   ├── UploadForm.tsx  # Drag & Drop upload
    │   └── DownloadBox.tsx # Download interface
    ├── /public
    ├── next.config.js
    └── package.json
```

---

## 🔌 API Endpoints

The backend exposes a minimalistic API to handle the handshake between the client and the raw socket server.

### 1. Upload File
Initiates the file transfer and allocates a port.

* **URL:** `/upload`
* **Method:** `POST`
* **Content-Type:** `multipart/form-data`
* **Response:**
```json
{
  "port": 8081
}
```

### 2. Download File
Streams the file content directly from the dedicated thread.

* **URL:** `/download/<port>`
* **Method:** `GET`
* **Description:** Connects to the dynamically assigned port returned by the upload endpoint to stream the file.

---

## 💻 Getting Started Locally

Follow these steps to run PeerPass on your local machine.

### Prerequisites
* **Java:** JDK 8 or higher installed (`java -version`)
* **Node.js:** Installed with npm (`node -v`)

### 1. Run the Backend
Navigate to the server directory, compile the Java source files, and start the application.

```bash
cd server
# Compile all Java files
javac Main.java

# Run the Main class
java Main
```

The server should now be running (default usually on port 8080 or similar, check console output).

### 2. Run the Frontend
Open a new terminal, navigate to the client directory, install dependencies, and start the Next.js dev server.

```bash
cd client
# Install dependencies
npm install

# Start the development server
npm run dev
```

### 3. Usage
1. Open your browser and go to `http://localhost:3000`.
2. Upload a file via the UI.
3. Use the generated link/port to download the file in a separate tab or browser.

---

## 🛡 Disclaimer

This project uses raw sockets and custom HTTP parsing for educational purposes to demonstrate multithreading and networking. It handles file streams directly in memory/threads and is not intended for production storage.

---

## 📄 License

MIT
