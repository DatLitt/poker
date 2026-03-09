import "./CommunityCards.css";
import Card from "../Card/Card";

interface CommunityCardsProps {
  cards: (string | null)[];
}

function CommunityCards({ cards }: CommunityCardsProps) {
  return (
    <div className="communityCardsContainer">
      <div className="flop">
        <Card cardName={cards[0]} />
        <Card cardName={cards[1]} />
        <Card cardName={cards[2]} />
      </div>
      <div className="turn-river">
        <Card cardName={cards[3]} />
        <Card cardName={cards[4]} />
      </div>
    </div>
  );
}

export default CommunityCards;
