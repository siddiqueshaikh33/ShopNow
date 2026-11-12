import React from "react";

function PopupModal({ title, onClose, children }) {
  return (
    <div className="fixed inset-0 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 z-50">
      <div className="bg-white w-full max-w-[550px] max-h-[90vh] overflow-y-auto p-6 sm:p-8 rounded-3xl shadow-2xl border border-slate-100 relative">
        <button
          type="button"
          onClick={onClose}
          className="absolute top-4 right-4 text-slate-400 hover:text-slate-800 text-lg font-bold w-8 h-8 rounded-full flex items-center justify-center hover:bg-slate-100 transition cursor-pointer"
          aria-label="Close modal"
        >
          ✕
        </button>

        <h2 className="text-xl font-bold text-slate-900 tracking-tight mb-4 pr-6">
          {title}
        </h2>

        {children}
      </div>
    </div>
  );
}

export default PopupModal;
