

function AdminDashboardCard({ title, subtitle, buttonText, onClick }) {
  return (
    <div className="w-full h-40 bg-blue-200  p-5 rounded-lg  flex flex-col justify-center items-center hover:border-blue-500 hover:border-2 transition-border  ease-in-out">
      <button
        className="w-auto h-auto bg-blue-500 text-white px-4 py-2 rounded"
        onClick={onClick}
      >
        {buttonText}
      </button>
      <p className="m-2 font-bold">{title}</p>

      <div className="m-1 text-gray-600">
        <h1 className="font-semibold">{subtitle}</h1>
      </div>

      <div>
      </div>
    </div>
  );
}

export default AdminDashboardCard;
