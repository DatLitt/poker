import "./Pot.css";

const Pot = ({ amount }: { amount: number }) => {
  return (
    <div className="pot">
      <span>Pot: ${amount}</span>
    </div>
  );
};

export default Pot;
