import "./Seat.css";
type Props = {
  player: string | null;
  className?: string;
};

export default function Seat({ player, className }: Props) {
  return <div className={className}>{player ?? "Empty"}</div>;
}
