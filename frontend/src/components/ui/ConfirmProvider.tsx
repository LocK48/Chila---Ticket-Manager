import {
  createContext,
  useContext,
  useRef,
  useState,
  type ReactNode,
} from "react";

type Choice = { label: string; value: string; primary?: boolean };
type DialogOptions = {
  title: string;
  message: string;
  confirmLabel?: string;
  destructive?: boolean;
  inputLabel?: string;
  placeholder?: string;
  choices?: Choice[];
};
type DialogRequest = DialogOptions & {
  mode: "confirm" | "prompt" | "choose";
  resolve: (value: string | boolean | null) => void;
};
type ConfirmApi = {
  confirm: (options: DialogOptions) => Promise<boolean>;
  prompt: (options: DialogOptions) => Promise<string | null>;
  choose: (
    options: DialogOptions & { choices: Choice[] },
  ) => Promise<string | null>;
};
const ConfirmContext = createContext<ConfirmApi | null>(null);

export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [dialog, setDialog] = useState<DialogRequest | null>(null);
  const [value, setValue] = useState("");
  const initialFocus = useRef<HTMLElement | null>(null);
  function open(
    mode: DialogRequest["mode"],
    options: DialogOptions,
  ): Promise<string | boolean | null> {
    initialFocus.current =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;
    setValue("");
    return new Promise((resolve) => setDialog({ ...options, mode, resolve }));
  }
  function close(result: string | boolean | null) {
    if (!dialog) return;
    dialog.resolve(result);
    setDialog(null);
    queueMicrotask(() => initialFocus.current?.focus());
  }
  const api: ConfirmApi = {
    confirm: (options) => open("confirm", options) as Promise<boolean>,
    prompt: (options) => open("prompt", options) as Promise<string | null>,
    choose: (options) => open("choose", options) as Promise<string | null>,
  };
  return (
    <ConfirmContext.Provider value={api}>
      {children}
      {dialog && (
        <div
          className="confirm-overlay"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget)
              close(dialog.mode === "confirm" ? false : null);
          }}
        >
          <section
            className="confirm-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="confirm-title"
            aria-describedby="confirm-message"
            onKeyDown={(event) => {
              if (event.key === "Escape")
                close(dialog.mode === "confirm" ? false : null);
            }}
          >
            <div
              className={`confirm-icon${dialog.destructive ? " is-danger" : ""}`}
              aria-hidden="true"
            >
              {dialog.destructive ? "!" : "?"}
            </div>
            <h2 id="confirm-title">{dialog.title}</h2>
            <p id="confirm-message">{dialog.message}</p>
            {dialog.mode === "prompt" && (
              <label className="confirm-input-label">
                {dialog.inputLabel ?? "Name"}
                <input
                  autoFocus
                  value={value}
                  placeholder={dialog.placeholder}
                  onChange={(event) => setValue(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") close(value.trim() || null);
                  }}
                />
              </label>
            )}
            <div className="confirm-actions">
              <button
                className="secondary-button"
                autoFocus={dialog.mode !== "prompt"}
                onClick={() => close(dialog.mode === "confirm" ? false : null)}
              >
                Cancel
              </button>
              {dialog.mode === "choose" ? (
                dialog.choices?.map((choice) => (
                  <button
                    key={choice.value}
                    className={
                      choice.primary
                        ? "primary-button compact"
                        : "secondary-button"
                    }
                    onClick={() => close(choice.value)}
                  >
                    {choice.label}
                  </button>
                ))
              ) : (
                <button
                  className={`primary-button compact${dialog.destructive ? " danger-button" : ""}`}
                  onClick={() =>
                    close(
                      dialog.mode === "confirm" ? true : value.trim() || null,
                    )
                  }
                >
                  {dialog.confirmLabel ??
                    (dialog.mode === "prompt" ? "Continue" : "Confirm")}
                </button>
              )}
            </div>
          </section>
        </div>
      )}
    </ConfirmContext.Provider>
  );
}

export function useConfirm() {
  const context = useContext(ConfirmContext);
  if (!context)
    throw new Error("useConfirm must be used within ConfirmProvider");
  return context;
}
