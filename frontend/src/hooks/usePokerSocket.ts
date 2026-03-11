import { useEffect } from "react";
import { socket } from "../socket/socket";
import type { Dispatch, SetStateAction } from "react";
export function usePokerSocket(
  setSeats: Dispatch<SetStateAction<(string | null)[]>>,
  setYourSeat: Dispatch<SetStateAction<number>>,
  setTableFull: Dispatch<SetStateAction<boolean>>,
  setCountdown: Dispatch<SetStateAction<number | null>>,
  setGameState: Dispatch<
    SetStateAction<"waiting" | "countdown" | "playing" | "spectating">
  >,
  setPlayerCards: Dispatch<SetStateAction<string[]>>,
  setCommunityCards: Dispatch<SetStateAction<(string | null)[]>>,
  setActionAllowed: Dispatch<SetStateAction<string[]>>,
  SetPot: Dispatch<SetStateAction<number>>,
  setPlayerTurn: Dispatch<SetStateAction<number | null>>,
) {
  useEffect(() => {
    const handler = (event: MessageEvent) => {
      const data = JSON.parse(event.data);
      console.log(data);

      switch (data.type) {
        case "table_state":
          setSeats(data.seats);
          setYourSeat(data.yourSeat);
          setTableFull(false);
          setCommunityCards([null, null, null, null, null]);
          SetPot(data.pot);
          break;
        case "table_full":
          setTableFull(true);
          break;
        case "game_countdown":
          setCountdown(data.seconds);
          setGameState("countdown");
          console.log("Game starts in", data.seconds, "seconds");
          break;
        case "spectator_mode":
          setGameState("spectating");
          break;
        case "deal_cards":
          setGameState("playing");

          console.log("Your cards:", data.cards);
          setPlayerCards(data.cards);
          break;
        case "community_cards":
          setCommunityCards(data.cards);
          break;
        case "player_turn":
          setPlayerTurn(data.seat);
          setActionAllowed(data.allowedActions);
          SetPot(data.pot);
          break;
        default:
          console.warn("Unknown message type:", data.type);
      }
    };

    socket.addEventListener("message", handler);

    return () => {
      socket.removeEventListener("message", handler);
    };
  }, []);
}
