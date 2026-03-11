import "./ActionButtons.css";

function ActionButtons({ actionAllowed }: { actionAllowed: string[] }) {
  return (
    <div className="actionButtons">
      <button disabled={!actionAllowed?.includes("fold")}>Fold</button>
      <button disabled={!actionAllowed?.includes("check")}>Check</button>
      <button disabled={!actionAllowed?.includes("call")}>Call</button>
      <button disabled={!actionAllowed?.includes("raise")}>Raise</button>
    </div>
  );
}

export default ActionButtons;
