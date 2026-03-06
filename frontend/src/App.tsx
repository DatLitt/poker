import { socket } from "./socket/socket";
import { useEffect } from "react";
import PokerTable from "./components/PokerTable/PokerTable";
import "./App.css";
function App() {
  useEffect(() => {
    socket.onopen = () => {
      console.log("connected");

      socket.send(
        JSON.stringify({
          type: "join_table",
          name: "Player123",
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
