import { useEffect } from "react";
import { socket } from "../socket/socket";
import type { Dispatch, SetStateAction } from "react";
export function usePokerSocket(
  setSeats: Dispatch<SetStateAction<(string | null)[]>>,
  setYourSeat: Dispatch<SetStateAction<number>>,
  setTableFull: Dispatch<SetStateAction<boolean>>,
  setCountdown: Dispatch<SetStateAction<number | null>>,
  setGameState: Dispatch<SetStateAction<"waiting" | "countdown" | "playing">>,
  setPlayerCards: Dispatch<SetStateAction<string[]>>,
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
          break;
        case "table_full":
          setTableFull(true);
          break;
        case "game_countdown":
          setCountdown(data.seconds);
          setGameState("countdown");
          console.log("Game starts in", data.seconds, "seconds");
          break;
        // case "game_start":
        //   setGameState("playing");
        //   break;
        case "deal_cards":
          setGameState("playing");
          console.log("Your cards:", data.cards);
          setPlayerCards(data.cards);
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
