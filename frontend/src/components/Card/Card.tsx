import "./Card.css";

const Card = ({ cardName }: { cardName: string | null }) => {
  const src = cardName ? `/${cardName}.png` : "/Back.png";
  return (
    <div className="card">
      <img src={src} alt={cardName ?? "cardback"} />
    </div>
  );
};

export default Card;
