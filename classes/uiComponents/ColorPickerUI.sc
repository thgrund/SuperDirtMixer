ColorPickerUI {
	var func;
	var color;
	var hsv;
	var color_picker_view; // Main View
	var v_slider;
	var v_slider_position;
	var sv_slider;
	var hex_text_field;
	var helper;
	var binded_function;
	var margin; // Slider half dead zone

	// HSV to Color function

	*new { | initFunc |
        ^super.new.init(initFunc);
    }

    init { | initFunc |
		func = initFunc;

	    color = Color.gray(0.85);
	    hsv = [ 0, 0, 0.85 ];

	    color_picker_view = UserView(); // Main View
		// Resizing adjustments
		color_picker_view.onResize = { | view |
			margin = ( view.bounds.width * 0.05 );
			v_slider.refresh;
			sv_slider.refresh;
		};

		v_slider = UserView();
		v_slider_position = 0;

		sv_slider = UserView();

		hex_text_field = TextField().string_("#D9D9D9");

		helper = UserView();

		binded_function = nil;
    }

    hsvToColor { | hsv |

		var color = Color( 0, 0, 0 );

		var h = hsv[0];
		var s = hsv[1];
		var v = hsv[2];

		var c = s * v;
		var x = c * ( 1 - ( ( h/60.0 )%2.0 -1 ).abs );
		var m = v - c;

		if( ( h >= 0 ) && ( h < 60 ), {
			color.red = c;
			color.green = x;
			color.blue = 0;
		} );

		if( ( h >= 60 ) && ( h < 120 ), {
			color.red = x;
			color.green = c;
			color.blue = 0;
		} );

		if( ( h >= 120 ) && ( h < 180 ), {
			color.red = 0;
			color.green = c;
			color.blue = x;
		} );

		if( ( h >= 180 ) && ( h < 240 ), {
			color.red = 0;
			color.green = x;
			color.blue = c;
		} );

		if( ( h >= 240 ) && ( h < 300 ), {
			color.red = x;
			color.green = 0;
			color.blue = c;
		} );

		if( ( h >= 300 ) && ( h < 360 ), {
			color.red = c;
			color.green = 0;
			color.blue = x;
		} );

		color.red = color.red + m;
		color.green = color.green + m;
		color.blue = color.blue + m;

		^color;
	}

	isValidHexColor { |str|
		var hexPattern = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";
		^hexPattern.matchRegexp(str);
	}

	setColorFromHexString {| str |

		if (this.isValidHexColor(str), {

			color = Color.fromHexString(str);

			hsv[0] = color.asHSV[0] * 360;
			hsv[1] = color.asHSV[1];
			hsv[2] = color.asHSV[2];

			v_slider_position = color.asHSV[0];
			helper.refresh;
			sv_slider.refresh;
			v_slider.refresh;
		});
	}

	createUI {
// Hue Slider setup
		v_slider.background_( Color.black );
		v_slider.drawFunc_( { | view |
			Pen.width = 1;

			if (margin.notNil, {

				(view.bounds.width - (margin * 2)).do( { |index|
					Pen.strokeColor_(
						this.hsvToColor(
							[
								index.linlin(
									0,
									(view.bounds.width - (margin * 2)),
									0,
									360
								),
								hsv[1],
								hsv[2]
							];
						);
					);

					Pen.moveTo( Point(index + margin, margin) );
					Pen.lineTo( Point(index + margin, view.bounds.height - margin) );
					Pen.stroke;
				});

				Pen.addRect(
					Rect(
						margin / 2 + (view.bounds.width - (margin * 2)) * v_slider_position + margin,
						margin / 2,
						margin,
						view.bounds.height - margin
					)
				);

				Pen.fillAxialGradient(
					Point(margin / 2 + (view.bounds.width - (margin * 2)) * v_slider_position + margin, 0),
					Point(margin / 2 + (view.bounds.width - (margin * 2)) * v_slider_position + (margin * 2), 0),
					Color(0, 0, 0, 1),
					Color(0, 0, 0, 0)
				);

				Pen.addRect(
					Rect(
						margin / 2 + (view.bounds.width - (margin * 2)) * v_slider_position - margin,
						margin / 2,
						margin,
						view.bounds.height - margin
					)
				);

				Pen.fillAxialGradient(
					Point(margin / 2 + (view.bounds.width - (margin * 2)) * v_slider_position - margin, 0),
					Point(margin / 2 + (view.bounds.width - (margin * 2)) * v_slider_position, 0),
					Color(0, 0, 0, 0),
					Color(0, 0, 0, 1)
				);

				Pen.fillColor_(color);
				Pen.fillRect(
					Rect(
						margin / 2 + (view.bounds.width - (margin * 2)) * v_slider_position,
						margin / 2,
						margin,
						view.bounds.height - margin
					)
				);
			});
		});

		v_slider.mouseDownAction_( { | view, x, y |
			if (x >= margin, {
				if (x <= (view.bounds.width - margin), {
					v_slider_position = x.linlin(margin, view.bounds.width - margin, 0, 1);
					x = x.linlin(margin, view.bounds.width - margin, 0, 360);
					hsv[0] = x;
					color = this.hsvToColor(hsv);
					if (binded_function != nil, { binded_function.value(color) });
					v_slider.refresh;
					helper.refresh;
					sv_slider.refresh;
					hex_text_field.string = color.hexString;
				});
			});
		});

		v_slider.mouseMoveAction_(v_slider.mouseDownAction);

		// Saturation Slider setup
		sv_slider.background_( Color.black );
		sv_slider.drawFunc_( { | view |

			if (margin.notNil, {
				( view.bounds.width - ( margin * 2 ) ).do( { | index_x |
					Pen.addRect(
						Rect(
							margin + index_x,
							margin,
							1,
							view.bounds.height - ( margin * 2 )
						)
					);
					Pen.fillAxialGradient(
						Point( 0, margin ),
						Point( 0, view.bounds.height - ( margin * 2 ) ),
						Color.black,
						this.hsvToColor(
							[
								hsv[0],
								index_x.linlin( 0, view.bounds.width, 0, 1 ),
								1
							]
						)
					);
				} );

				Pen.addWedge(
					Point(
						view.bounds.width - ( margin * 2 ) * hsv[1] + margin,
						view.bounds.height - ( margin * 2 ) * hsv[2] + margin,
					),
					margin * 2,
					0,
					360
				);
				Pen.fillRadialGradient(
					Point(
						view.bounds.width - ( margin * 2 ) * hsv[1] + margin,
						view.bounds.height - ( margin * 2 ) * hsv[2] + margin,
					),
					Point(
						view.bounds.width - ( margin * 2 ) * hsv[1] + margin,
						view.bounds.height - ( margin * 2 ) * hsv[2] + margin,
					),
					margin,
					margin * 1.5,
					Color( 0, 0, 0, 1 ),
					Color( 0, 0, 0, 0 )
				);

				Pen.fillColor_( color );
				Pen.addWedge(
					Point(
						view.bounds.width - ( margin * 2 ) * hsv[1] + margin,
						view.bounds.height - ( margin * 2 ) * hsv[2] + margin,
					),
					margin,
					0,
					360
				);
				Pen.fill;
			});
		} );

		sv_slider.mouseDownAction_( { | view, x, y |

			case
			{ x < margin } { hsv[1] = 0 }
			{ x > ( view.bounds.width + margin ) } { hsv[1] = 1 }
			{ ( ( x >= margin ) && ( x <= ( view.bounds.width - margin ) ) ) } {
				hsv[1] = x.linlin( margin, view.bounds.width - margin, 0, 1 );
			};

			case
			{ y < margin } { hsv[2] = 0 }
			{ y > ( view.bounds.height + margin ) } { hsv[2] = 1 }
			{ ( ( y >= margin ) && ( y <= ( view.bounds.height - margin ) ) ) } {
				hsv[2] = y.linlin( margin, view.bounds.height - margin, 0, 1 );
			};

			color = this.hsvToColor( hsv );
			if( binded_function != nil, { binded_function.value( color ) } );
			helper.refresh;
			sv_slider.refresh;
			v_slider.refresh;
			hex_text_field.string = color.hexString;
		} );
		sv_slider.mouseMoveAction_( sv_slider.mouseDownAction );

		helper.drawFunc_( { | view |
			//EXPOSE COLOR HERE
			func.value(color);

			Pen.fillColor_( color );
			Pen.fillRect(
				Rect(
					0,
					0,
					view.bounds.width,
					view.bounds.height
				)
			)
		} );

		hex_text_field.keyUpAction = {
			|tf|

			this.setColorFromHexString(tf.value);
		};

		hex_text_field.align_(\center);
		// Background Color
		color_picker_view.background_( Color.black );

	    color_picker_view.layout_(
		    VLayout(
			    [HLayout( [helper, stretch: 1], [sv_slider, stretch: 3]), stretch: 3],
			    [v_slider, stretch: 1],
			    [hex_text_field, stretch: 1]
		    ),
	    );

	    color_picker_view.addUniqueMethod( \bindFunction, { | object, function | binded_function = function } );

	    ^color_picker_view;
	}
}