function AdminDashboardCard({ title, subtitle, buttonText, onClick }) {
  return (
    <div className="w-full min-h-44 h-auto bg-white border border-slate-200/90 shadow-sm p-6 rounded-2xl flex flex-col justify-between items-center text-center hover:shadow-md transition-all">
      <button
        type="button"
        className="px-5 py-2.5 bg-slate-900 hover:bg-[#062828] text-white font-bold text-xs rounded-xl transition shadow-xs cursor-pointer active:scale-[0.98]"
        style={{ borderBottom: "2px solid #ADEFD1" }}
        onClick={onClick}
      >
        {buttonText}
      </button>
      <p className="mt-3 font-bold text-slate-900 text-sm sm:text-base">{title}</p>

      <div className="mt-1 text-slate-500">
        <span className="font-medium text-xs sm:text-sm">{subtitle}</span>
      </div>
    </div>
  );
}

export default AdminDashboardCard;
