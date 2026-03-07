import Seat from "../Seat/Seat";
import { usePokerSocket } from "../../hooks/usePokerSocket";
import "./PokerTable.css";
import { useState } from "react";

export default function PokerTable() {
  const [yourId, setYourId] = useState(1); //user's seat before rotate
  const [seats, setSeats] = useState<(string | null)[]>(Array(8).fill(null));
  const [tableFull, setTableFull] = useState(false);
  const [countdown, setCountdown] = useState<number | null>(null);
  const [gameState, setGameState] = useState<
    "waiting" | "countdown" | "playing"
  >("waiting");
  const [playerCards, setPlayerCards] = useState<string[]>([]);
  usePokerSocket(
    setSeats,
    setYourId,
    setTableFull,
    setCountdown,
    setGameState,
    setPlayerCards,
  );

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
      {seats.filter((seat) => seat !== null).length === 1 && (
        <div className="waiting">Waiting for players...</div>
      )}
      {gameState === "countdown" && (
        <div className="countdown">Game starts in {countdown}</div>
      )}
      {tableFull && <div className="table-full">Table is full</div>}
      {!tableFull &&
        displaySeats.map((player, i) => (
          <Seat key={i} player={player} className={`seat seat${i + 1}`} />
        ))}
      {gameState === "playing" && (
        <div className="player-cards">Your Cards: {playerCards.join(", ")}</div>
      )}
    </div>
  );
}
