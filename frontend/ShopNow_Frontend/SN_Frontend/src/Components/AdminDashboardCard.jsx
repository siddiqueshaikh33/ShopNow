function AdminDashboardCard({ title, subtitle, buttonText, onClick }) {
  return (
    <div className="w-full min-h-40 h-auto bg-blue-200 p-4 sm:p-5 rounded-lg flex flex-col justify-center items-center hover:border-blue-500 hover:border-2 transition-border ease-in-out text-center">
      <button
        className="w-auto h-auto bg-blue-500 text-white px-4 py-2 rounded"
        onClick={onClick}
      >
        {buttonText}
      </button>
      <p className="m-2 font-bold text-sm sm:text-base">{title}</p>

      <div className="m-1 text-gray-600">
        <h1 className="font-semibold text-xs sm:text-sm">{subtitle}</h1>
      </div>

      <div></div>
    </div>
  );
}

export default AdminDashboardCard;
