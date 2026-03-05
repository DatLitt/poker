import { socket } from "./socket";
import { useEffect } from "react";
import PokerTable from "./components/PokerTable/PokerTable";
function App() {
  useEffect(() => {
    socket.emit("join_table", {
      name: "Dat",
    });
  }, []);

  return (
    <div>
      <PokerTable />
    </div>
  );
}

export default App;
