import "./Pot.css";

const Pot = ({ amount }: { amount: number }) => {
  return (
    <div className="pot">
      <span>Pot: ${amount.toFixed(2)}</span>
    </div>
  );
};

export default Pot;
