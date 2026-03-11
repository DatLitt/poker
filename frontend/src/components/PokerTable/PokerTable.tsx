import Seat from "../Seat/Seat";
import { usePokerSocket } from "../../hooks/usePokerSocket";
import "./PokerTable.css";
import { useState } from "react";
import Card from "../Card/Card";
import CommunityCards from "../CommunityCards/CommunityCards";
import ActionButtons from "../ActionButtons/ActionButtons";
import Pot from "../Pot/Pot";

export default function PokerTable() {
  const [yourId, setYourId] = useState(1); //user's seat before rotate
  const [seats, setSeats] = useState<(string | null)[]>(Array(8).fill(null));
  const [pot, setPot] = useState(0);
  const [tableFull, setTableFull] = useState(false);
  const [countdown, setCountdown] = useState<number | null>(null);
  const [gameState, setGameState] = useState<
    "waiting" | "countdown" | "playing" | "spectating"
  >("waiting");
  const [playerCards, setPlayerCards] = useState<string[]>([]);
  const [communityCards, setCommunityCards] = useState<(string | null)[]>([
    null,
    null,
    null,
    null,
    null,
  ]);
  const [actionAllowed, setActionAllowed] = useState<string[]>([]);
  const [playerTurn, setPlayerTurn] = useState<number | null>(null);
  const [winner, setWinner] = useState<number | null>(null);
  usePokerSocket(
    setSeats,
    setYourId,
    setTableFull,
    setCountdown,
    setGameState,
    setPlayerCards,
    setCommunityCards,
    setActionAllowed,
    setPot,
    setPlayerTurn,
    setWinner,
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
      {gameState === "spectating" && (
        <div className="spectating">You are spectating</div>
      )}
      {gameState === "playing" && (
        <>
          <div className="player-cards">
            <Card cardName={playerCards[0]} />
            <Card cardName={playerCards[1]} />
          </div>
          <CommunityCards cards={communityCards} />
          <ActionButtons
            actionAllowed={playerTurn === yourId ? actionAllowed : []}
          />
          <Pot amount={pot} />
          {playerTurn !== null && (
            <div className="player-turn">Player {playerTurn + 1}'s turn</div>
          )}
          {winner !== null && (
            <div className="winner">Player {winner + 1} wins!</div>
          )}
        </>
      )}
    </div>
  );
}
