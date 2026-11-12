import { ArrowLeft } from "lucide-react";
import { useNavigate } from "react-router-dom";

function BackNav({ to, label = "Back", className = "" }) {
  const navigate = useNavigate();

  const handleBack = () => {
    if (to) {
      navigate(to);
    } else if (window.history.state && window.history.state.idx > 0) {
      navigate(-1);
    } else {
      navigate("/dashboard");
    }
  };

  return (
    <div className={`flex items-center ${className}`}>
      <button
        type="button"
        onClick={handleBack}
        className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold text-slate-700 bg-white border border-slate-200/90 shadow-2xs hover:bg-slate-50 hover:text-slate-950 hover:border-slate-300 transition-all cursor-pointer"
        style={{
          borderLeft: "3px solid #ADEFD1",
        }}
        aria-label={label}
      >
        <ArrowLeft className="w-3.5 h-3.5 text-slate-600" />
        <span>{label}</span>
      </button>
    </div>
  );
}

export default BackNav;

