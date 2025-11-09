export interface EventProcessor {
  moveMouseToElement: (element: Element, duration: number) => void;
  clickElement: (element: Element) => void;
  moveToElementAndClick: (element: Element, duration: number) => void;
  tapElement: (element: Element) => void;
  doubleTapElement: (element: Element) => void;
  flickToElement: (element: Element, duration: number) => void;
  // used for executing a serialized set of JSON events
  runSerializedEvents: (events: Array<any>) => void;
}


export interface Shv {
  proc: EventProcessor;

  find(selector: string): Element[];

  findAllIn(parent: string | Element, childSelector: string, childIndex: number): Element[];

  findIn(parent: string | Element, childSelector: string, childIndex: number, parentIndex: number): Element | null;

  findOne(selector: string): Element | null;

  click(selectorOrTarget: string | Element, duration: number): void;

  hover(selectorOrTarget: string | Element, duration: number): void;

  clickFast(selectorOrTarget: string | Element): void;

  clickSlow(selectorOrTarget: string | Element): void;

  waitUntilFound<R>(provider: (shv: Shv) => R, name: string | null, timeoutMs: number | null, intervalMs: number | null): Promise<R>;
}
