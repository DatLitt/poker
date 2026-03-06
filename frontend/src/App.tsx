import { socket } from "./socket/socket";
import { useEffect } from "react";
import PokerTable from "./components/PokerTable/PokerTable";
import "./App.css";

function randomName() {
  const adjectives = ["Lucky", "Crazy", "Silent", "Wild", "Golden", "Swift"];
  const animals = ["Tiger", "Shark", "Fox", "Wolf", "Dragon", "Falcon"];

  const adj = adjectives[Math.floor(Math.random() * adjectives.length)];
  const animal = animals[Math.floor(Math.random() * animals.length)];
  const num = Math.floor(Math.random() * 100);

  return `${adj}${animal}${num}`;
}

function App() {
  useEffect(() => {
    socket.onopen = () => {
      console.log("connected");

      socket.send(
        JSON.stringify({
          type: "join_table",
          name: randomName(),
        }),
      );
    };
  }, []);

  return (
    <div className="container">
      <PokerTable />
    </div>
  );
}

export default App;
