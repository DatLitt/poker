import "./Seat.css";
type Props = {
  player: string | null;
  className?: string;
  money: number | null;
};

export default function Seat({ player, className, money }: Props) {
  return (
    <div className={className}>
      <p className="player_name">{player || "Empty"}</p>
      {money != null && <p className="money">$ {money}</p>}
    </div>
  );
}
