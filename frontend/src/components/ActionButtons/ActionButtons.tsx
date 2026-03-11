import { useState } from "react";
import { socket } from "../../socket/socket";
import "./ActionButtons.css";

type PlayerActionMessage = {
  type: "player_action";
  action: "fold" | "check" | "call" | "raise" | "all_in";
  amount?: number;
};
function ActionButtons({ actionAllowed }: { actionAllowed: string[] }) {
  const [raiseAmount, setRaiseAmount] = useState(0);

  function sendAction(action: "fold" | "check" | "call" | "raise" | "all_in") {
    const message: PlayerActionMessage = {
      type: "player_action",
      action: action,
    };

    if (action === "raise") {
      message.amount = raiseAmount;
    }

    socket.send(JSON.stringify(message));
  }

  return (
    <div className="actionButtons">
      <button
        disabled={!actionAllowed?.includes("fold")}
        onClick={() => sendAction("fold")}
      >
        Fold
      </button>

      <button
        disabled={!actionAllowed?.includes("check")}
        onClick={() => sendAction("check")}
      >
        Check
      </button>

      <button
        disabled={!actionAllowed?.includes("call")}
        onClick={() => sendAction("call")}
      >
        Call
      </button>
      <div>
        <input
          type="number"
          value={raiseAmount}
          onChange={(e) => setRaiseAmount(Number(e.target.value))}
          placeholder="Raise amount"
        />

        <button
          disabled={!actionAllowed?.includes("raise")}
          onClick={() => sendAction("raise")}
        >
          Raise
        </button>
      </div>
      <button
        disabled={!actionAllowed?.includes("all_in")}
        onClick={() => sendAction("all_in")}
      >
        All In
      </button>
    </div>
  );
}
export default ActionButtons;
