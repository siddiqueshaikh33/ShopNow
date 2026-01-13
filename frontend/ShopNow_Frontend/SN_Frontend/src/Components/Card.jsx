function Card({product, onAction}) {
  return (
    <div className="w-72 h-auto rounded-xl border-black border-1 bg-white shadow-md overflow-hidden transition hover:shadow-xl p-2">

      <img
        src={product.product_img}
        alt={product.name}
        className="h-50 w-full object-contain"
        loading="lazy"
      />

      <div className="pt-4 px-4 flex justify-between items-center">
        <h3 className="text-lg font-semibold text-black-800 truncate">
          {product.name}
        </h3>
        <h3 className="font-semibold text-md text-gray-800">
           Stock : {product.stock > 0 ? product.stock : "Out of Stock"}
        </h3>
      </div>
      <div className="px-4 mt-1 flex flex-col">
        <p className="text-md text-gray-600">
          {product.description}
        </p>
        <p className="mt-1 text-md font-semibold text-black-500 line-clamp-3">
          Price : ${product.price}
        </p>
        <button className="mt-4 text-lg font-semibold text-blue-600 bg-blue-100 px-3 py-2 rounded-md hover:bg-blue-200 transition w-full" onClick={()=> onAction(product)}>
         Add To Cart
        </button>
      </div>
    </div>
  );
}

export default Card;
