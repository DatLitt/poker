import Seat from "../Seat/Seat";
import { useEffect } from "react";
import { socket } from "../../socket";
import "./PokerTable.css";
import { useState } from "react";

export default function PokerTable() {
  const [yourId, setYourId] = useState(1); //user's seat before rotate
  const [seats, setSeats] = useState<(string | null)[]>([
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
  ]);

  useEffect(() => {
    socket.on("table_state", (data) => {
      setSeats(data.seats);
      setYourId(data.yourId);
    });
  }, []);

  function rotateSeats(seats: (string | null)[], yourId: number) {
    const targetSeat = 4; // seat bottom (UI position)

    const shift = targetSeat - yourId;

    return seats.map((_, i) => {
      const index = (i - shift + seats.length) % seats.length;
      return seats[index];
    });
  }

  const displaySeats = rotateSeats(seats, yourId);

  return (
    <div className="table">
      {displaySeats.map((player, i) => (
        <Seat key={i} player={player} className={`seat seat${i + 1}`} />
      ))}
    </div>
  );
}
