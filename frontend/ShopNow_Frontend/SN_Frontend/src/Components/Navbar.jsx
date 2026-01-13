import logo from '../Assets/logo.jpg';
import CartIcon from './CartIcon.jsx';

function Navbar({user, count}) {
  return (
    <nav className="sticky top-0 z-50">
    <div className="flex bg-[#ADEFD1] w-full pt-3 pb-3 px-4 lg:px-15 flex-row justify-between items-center">
        <div className="flex items-center">
            <img src={logo} alt="Logo" className="w-12 h-12 rounded-full object-contain mr-2"/>
            <p className="text-xl lg:text-3xl font-bold">ShopNow!</p>
        </div>
        <div className="flex items-center gap-6">
            <CartIcon count={count} />
        <div className="flex border-1 rounded-4xl border-black-100 bg-white">
            <img src={logo} alt="logo" className="w-8 h-8 rounded-full object-cover m-2"/>
            <p className="m-2 p-1 text-black text-md font-semibold">{user}</p>
        </div>
        </div>
    </div>
</nav>
  )
}

export default Navbar