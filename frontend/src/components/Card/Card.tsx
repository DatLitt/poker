import "./Card.css";

const Card = ({ cardName }: { cardName: string }) => {
  return (
    <div className="card">
      <img src={`/${cardName}.png`} alt={cardName} />
    </div>
  );
};

export default Card;
