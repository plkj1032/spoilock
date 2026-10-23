import "../../styles/Button.css";

export default function Button({
  children,
  variant = "primary",
  className = "",
  type = "button",
  ...props
}) {
  const buttonClassName = ["button", `button-${variant}`, className]
    .filter(Boolean)
    .join(" ");

  return (
    <button type={type} className={buttonClassName} {...props}>
      {children}
    </button>
  );
}
