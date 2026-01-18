import { FaGithub, FaLinkedin, FaInstagram } from "react-icons/fa";

function Footer() {
  return (
    <footer className="bg-[#ADEFD1] text-gray-800 mt-12">
      <div className="max-w-7xl mx-auto px-6 py-8">

        {/* Top Section */}
        <div className="flex flex-col md:flex-row justify-between items-center gap-6">

          {/* Brand */}
          <div className="text-center md:text-left">
            <h2 className="text-2xl font-bold">ShopNow!</h2>
            <p className="text-sm text-gray-700">
              Smart shopping made simple.
            </p>
          </div>

          {/* Social Icons */}
          <div className="flex gap-5 text-2xl">
            <a href="#" className="hover:text-gray-600 transition">
              <FaGithub />
            </a>
            <a href="#" className="hover:text-gray-600 transition">
              <FaLinkedin />
            </a>
            <a href="#" className="hover:text-gray-600 transition">
              <FaInstagram />
            </a>
          </div>
        </div>

        {/* Divider */}
        <div className="border-t border-gray-400 my-6"></div>

        {/* Bottom Section */}
        <div className="text-center text-sm space-y-2">
          <p>
            © {new Date().getFullYear()} <span className="font-semibold">ShopNow!</span>
            &nbsp;• All rights reserved
          </p>

          <p className="text-gray-700">
            Built with ❤️ using{" "}
            <span className="font-medium">
              React, Tailwind CSS, Spring Boot & MySQL
            </span>
          </p>

          <p className="text-gray-600">
            Built by <span className="font-semibold">Siddique Shaikh</span>
          </p>
        </div>

      </div>
    </footer>
  );
}

export default Footer;
