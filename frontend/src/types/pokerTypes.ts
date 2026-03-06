export type TableState = {
  type: "table_state";
  seats: (string | null)[];
  yourSeat: number;
};

export type DealCards = {
  type: "deal_cards";
  cards: string[];
};

export type ServerMessage = TableState | DealCards;
