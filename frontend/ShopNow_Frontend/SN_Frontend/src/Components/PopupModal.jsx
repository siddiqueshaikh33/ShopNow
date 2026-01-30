import React from "react";

function PopupModal({ title, onClose, children }) {
  return (
    <div className="fixed inset-0 flex items-center justify-center bg-black/50">
      <div className="bg-white w-[550px] p-6 rounded-xl shadow-lg relative">

        <button
          onClick={onClose}
          className="absolute top-3 right-3 text-gray-500 hover:text-red-600 text-xl font-bold"
        >
          ✕
        </button>

        <h2 className="text-xl font-semibold mb-4">{title}</h2>

        {children}
      </div>
    </div>
  );
}

export default PopupModal;
